// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.seed;

import static java.util.Calendar.YEAR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Connects to <a href="https://www.random.org/clients/http/" target="_top">random.org's old
 * API</a> (via HTTPS) and downloads a set of random bits to use as seed data.  It is generally
 * better to use the {@link DevRandomSeedGenerator} where possible, as it should be much quicker.
 * This seed generator is most useful on Microsoft Windows without Cygwin, and other platforms that
 * do not provide {@literal /dev/random}.</p>
 * <p>Random.org collects randomness from atmospheric noise using 9 radios, located at undisclosed
 * addresses in Dublin and Copenhagen and tuned to undisclosed AM/FM frequencies. (The secrecy is
 * intended to help prevent tampering with the output using a well-placed radio transmitter, and the
 * use of AM/FM helps ensure that any such tampering would cause illegal interference with
 * broadcasts and quickly attract regulatory attention.)</p>
 * <p>Random.org has two APIs: an <a href="https://www.random.org/clients/http/">old API</a> and a
 * <a href="https://api.random.org/json-rpc/1/">newer JSON-RPC API</a>. Since the new one requires
 * a key obtained from random.org, the old one is used by default. However, if you have a key, you
 * can provide it by calling {@link #setApiKey(UUID)}, and the new API will then be used.</p>
 * <p>Note that when using the old API, random.org limits the supply of free random numbers to any
 * one IP address; if you operate from a fixed address (at least if you use IPv4), you can <a
 * href="https://www.random.org/quota/">check
 * your quota and buy more</a>. On the new API, the quota is per key rather than per IP, and
 * commercial service tiers are to come in early 2018, shortly after the new API leaves beta.</p>
 * @author Daniel Dyer (old API)
 * @author Chris Hennick (new API)
 */
public enum RandomDotOrgSeedGenerator implements SeedGenerator {
  /**
   * This version of the client may make HTTP requests as fast as your computer is capable of
   * sending them. Since it is inherently spammy, it is recommended only when you know your usage is
   * light and/or no other source of randomness will do.
   */
  RANDOM_DOT_ORG_SEED_GENERATOR(false),

  /**
   * Upon a failed request, this version of the client waits 10 seconds before trying again. If
   * called again during that waiting period, throws {@link SeedException}. The {@link
   * DefaultSeedGenerator} uses this version.
   */
  DELAYED_RETRY(true);
  private static final String JSON_REQUEST_FORMAT = "{\"jsonrpc\":\"2.0\","
      + "\"method\":\"generateBlobs\",\"params\":{\"apiKey\":\"%s\",\"n\":1,\"size\":%d},\"id\":%d}";

  private static final AtomicLong REQUEST_ID = new AtomicLong(0);
  private static final AtomicReference<UUID> API_KEY = new AtomicReference<>(null);
  private static final JSONParser JSON_PARSER = new JSONParser();
  private static final int MAX_CACHE_SIZE = 625; // 5000 bits = 1/50 daily limit per API key
  private static final String BASE_URL = "https://www.random.org";
  /**
   * The URL from which the random bytes are retrieved (old API).
   */
  @SuppressWarnings("HardcodedFileSeparator") private static final String RANDOM_URL =
      BASE_URL + "/integers/?num={0,number,0}&min=0&max=255&col=1&base=16&format=plain&rnd=new";
  /**
   * Used to identify the client to the random.org service.
   */
  private static final String USER_AGENT = RandomDotOrgSeedGenerator.class.getName();
  /**
   * Random.org does not allow requests for more than 10k integers at once. This field is
   * package-visible for testing.
   */
  static final int GLOBAL_MAX_REQUEST_SIZE = 10000;
  private static final int RETRY_DELAY_MS = 10_000;
  private static final Lock cacheLock = new ReentrantLock();
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  private static volatile Calendar EARLIEST_NEXT_ATTEMPT = Calendar.getInstance(UTC);
  private static final Logger LOG = LoggerFactory.getLogger(RandomDotOrgSeedGenerator.class);
  private static volatile byte[] cache = new byte[MAX_CACHE_SIZE];
  private static volatile int cacheOffset = cache.length;
  private static volatile int maxRequestSize = GLOBAL_MAX_REQUEST_SIZE;

  private static final URL JSON_REQUEST_URL;
  /**
   * The proxy to use with random.org, or null to use the JVM default.
   */
  private static final AtomicReference<Proxy> proxy = new AtomicReference<>(null);

  static {
    EARLIEST_NEXT_ATTEMPT.add(YEAR, -1);
  }

  static {
    try {
      JSON_REQUEST_URL = new URL("https://api.random.org/json-rpc/1/invoke");
    } catch (MalformedURLException e) {
      // Should never happen.
      throw new RuntimeException(e);
    }
  }

  /**
   * If true, don't attempt to contact random.org again for RETRY_DELAY after an IOException
   */
  private final boolean useRetryDelay;

  RandomDotOrgSeedGenerator(final boolean useRetryDelay) {
    this.useRetryDelay = useRetryDelay;
  }

  /**
   * Sets the API key. If not null, random.org's JSON API is used. Otherwise, the old API is used.
   * @param apiKey An API key obtained from random.org.
   */
  public static void setApiKey(@Nullable UUID apiKey) {
    API_KEY.set(apiKey);
  }

  /**
   * Sets the proxy to use to connect to random.org. If null, the JVM default is used.
   * @param proxy a proxy, or null for the JVM default
   */
  public static void setProxy(@Nullable final Proxy proxy) {
    RandomDotOrgSeedGenerator.proxy.set(proxy);
  }

  private static HttpsURLConnection openConnection(final URL url) throws IOException {
    final Proxy currentProxy = proxy.get();
    return (HttpsURLConnection)
        ((currentProxy == null) ? url.openConnection() : url.openConnection(currentProxy));
  }

  /**
   * @param requiredBytes The preferred number of bytes to request from random.org. The
   *     implementation may request more and cache the excess (to avoid making lots of small
   *     requests). Alternatively, it may request fewer if the required number is greater than that
   *     permitted by random.org for a single request.
   * @throws IOException If there is a problem downloading the random bits.
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") private static void refreshCache(
      final int requiredBytes) throws IOException {
    HttpsURLConnection connection = null;
    cacheLock.lock();
    try {
      int numberOfBytes = Math.max(requiredBytes, cache.length);
      numberOfBytes = Math.min(numberOfBytes, maxRequestSize);
      if (numberOfBytes != cache.length) {
        cache = new byte[numberOfBytes];
        cacheOffset = numberOfBytes;
      }
      UUID currentApiKey = API_KEY.get();
      if (currentApiKey == null) {
        // Use old API.
        connection = openConnection(new URL(MessageFormat.format(RANDOM_URL, numberOfBytes)));
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream()))) {
          int index = -1;
          for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++index;
            if (index >= numberOfBytes) {
              LOG.warn("random.org sent more data than requested.");
              break;
            }
            cache[index] = (byte) Integer.parseInt(line, 16);
            // Can't use Byte.parseByte, since it expects signed
          }
          if (index < (cache.length - 1)) {
            throw new IOException(String.format(
                "Insufficient data received: expected %d bytes, got %d.", cache.length, index + 1));
          }
        }
      } else {
        // Use JSON API.
        connection = openConnection(JSON_REQUEST_URL);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (OutputStream out = connection.getOutputStream()) {
          out.write(String.format(JSON_REQUEST_FORMAT, currentApiKey, numberOfBytes * Byte.SIZE,
              REQUEST_ID.incrementAndGet()).getBytes(UTF8));
        }
        final JSONObject response;
        try (InputStream in = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(in)) {
          response = (JSONObject) JSON_PARSER.parse(reader);
        } catch (ParseException e) {
          throw new SeedException("Unparseable JSON response from random.org", e);
        }
        JSONObject error = (JSONObject) response.get("error");
        if (error != null) {
          throw new SeedException(error.toString());
        }
        JSONObject result = checkedGetObject(response, "result");
        JSONObject random = checkedGetObject(result, "random");
        Object data = random.get("data");
        if (data == null) {
          throw new SeedException("'data' missing from 'random': " + random);
        } else {
          String base64seed =
              (data instanceof JSONArray ? ((JSONArray) data).get(0) : data).toString();
          byte[] decodedSeed = DatatypeConverter.parseBase64Binary(base64seed);
          if (decodedSeed.length < numberOfBytes) {
            throw new SeedException(
                "Too few bytes returned: requested " + numberOfBytes + ", got " + base64seed);
          }
          System.arraycopy(decodedSeed, 0, cache, 0, numberOfBytes);
        }
        Number advisoryDelayMs = (Number) result.get("advisoryDelay");
        if (advisoryDelayMs != null) {
          // Wait RETRY_DELAY or the advisory delay, whichever is shorter
          EARLIEST_NEXT_ATTEMPT.setTime(new Date());
          EARLIEST_NEXT_ATTEMPT
              .add(Calendar.MILLISECOND, Math.min(advisoryDelayMs.intValue(), RETRY_DELAY_MS));
        }
      }
      cacheOffset = 0;
    } finally {
      cacheLock.unlock();
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private static JSONObject checkedGetObject(JSONObject parent, String key) {
    JSONObject child = (JSONObject) parent.get(key);
    if (child == null) {
      throw new SeedException("No '" + key + "' in: " + parent);
    }
    return child;
  }

  /**
   * Sets the maximum request size that we will expect random.org to allow. If more than {@link
   * #GLOBAL_MAX_REQUEST_SIZE}, will be set to that value instead.
   * @param maxRequestSize the new maximum request size in bytes.
   */
  public static void setMaxRequestSize(int maxRequestSize) {
    maxRequestSize = Math.min(maxRequestSize, GLOBAL_MAX_REQUEST_SIZE);
    final int maxNewCacheSize = Math.min(maxRequestSize, MAX_CACHE_SIZE);
    cacheLock.lock();
    try {
      final int sizeChange = maxNewCacheSize - cache.length;
      if (sizeChange > 0) {
        final byte[] newCache = new byte[maxNewCacheSize];
        final int newCacheOffset = cacheOffset + sizeChange;
        System.arraycopy(cache, cacheOffset, newCache, newCacheOffset, cache.length - cacheOffset);
        cache = newCache;
      }
      RandomDotOrgSeedGenerator.maxRequestSize = maxRequestSize;
    } finally {
      cacheLock.unlock();
    }
  }

  @Override @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  public void generateSeed(final byte[] seedData) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException("Not retrying so soon after an IOException");
    }
    final int length = seedData.length;
    cacheLock.lock();
    try {
      int count = 0;
      while (count < length) {
        if (cacheOffset < cache.length) {
          final int numberOfBytes = Math.min(length - count, cache.length - cacheOffset);
          System.arraycopy(cache, cacheOffset, seedData, count, numberOfBytes);
          count += numberOfBytes;
          cacheOffset += numberOfBytes;
        } else {
          refreshCache(length - count);
        }
      }
    } catch (final IOException ex) {
      EARLIEST_NEXT_ATTEMPT.setTime(new Date());
      EARLIEST_NEXT_ATTEMPT.add(Calendar.MILLISECOND, RETRY_DELAY_MS);
      throw new SeedException("Failed downloading bytes from " + BASE_URL, ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in an applet sandbox).
      throw new SeedException("SecurityManager prevented access to " + BASE_URL, ex);
    } finally {
      cacheLock.unlock();
    }
  }

  /**
   * Returns true if we cannot determine quickly (i.e. without I/O calls) that this SeedGenerator
   * would throw a {@link SeedException} if {@link #generateSeed(int)} or {@link
   * #generateSeed(byte[])} were being called right now.
   * @return true if this SeedGenerator will get as far as an I/O call or other slow operation in
   *     attempting to generate a seed immediately.
   */
  @Override public boolean isWorthTrying() {
    return !useRetryDelay || !EARLIEST_NEXT_ATTEMPT.after(Calendar.getInstance(UTC));
  }

  /**
   * Returns "https://www.random.org (with retry delay)" or "https://www.random.org (without retry
   * delay)".
   */
  @Override public String toString() {
    return BASE_URL + (useRetryDelay ? " (with retry delay)" : " (without retry delay)");
  }

  /**
   * Generates and returns a seed value for a random number generator as a new array.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }
}
