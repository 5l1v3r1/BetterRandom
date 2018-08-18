package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java8.util.SplittableRandom;
import javax.annotation.Nullable;

/**
 * Thread-safe PRNG that wraps a {@link ThreadLocal}&lt;{@link SplittableRandom}&gt;. Reseeding this
 * will only affect the calling thread, so this can't be used with a {@link RandomSeederThread}.
 * Instead, use a {@link ReseedingSplittableRandomAdapter}.
 * @author Chris Hennick
 */
@SuppressWarnings("ThreadLocalNotStaticFinal")
public class SplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final int SEED_LENGTH_BITS = Java8Constants.LONG_BYTES * 8;
  private static final long serialVersionUID = 2190439512972880590L;
  private transient ThreadLocal<SplittableRandom> splittableRandoms;
  private transient ThreadLocal<AtomicLong> entropyBits;
  private transient ThreadLocal<byte[]> seeds;
  private transient ThreadLocal<ByteBuffer> seedBuffers;

  /**
   * Use the provided seed generation strategy to create the seed for the master {@link
   * SplittableRandom}, which will be split to generate an instance for each thread.
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException if there is a problem generating a seed.
   */
  public SplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(Java8Constants.LONG_BYTES));
  }

  /**
   * Use the provided seed for the master {@link SplittableRandom}, which will be split to generate
   * an instance for each thread.
   * @param seed The seed. Must be 8 bytes.
   */
  public SplittableRandomAdapter(final byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  /**
   * Use the {@link DefaultSeedGenerator} to generate a seed for the master {@link
   * SplittableRandom}, which will be split to generate an instance for each thread.
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  public SplittableRandomAdapter() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(Java8Constants.LONG_BYTES));
  }

  /**
   * Use the provided seed for the master {@link SplittableRandom}, which will be split to generate
   * an instance for each thread.
   * @param seed The seed.
   */
  public SplittableRandomAdapter(final long seed) {
    super(seed);
    initSubclassTransientFields();
  }

  @Override protected boolean useParallelStreams() {
    return true;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  /** Returns the entropy count for the calling thread (it is separate for each thread). */
  @Override public long getEntropyBits() {
    return entropyBits.get().get();
  }

  @Override protected void debitEntropy(final long bits) {
    entropyBits.get().addAndGet(-bits);
  }

  @Override protected void creditEntropyForNewSeed(final int seedLength) {
    if (entropyBits != null) {
      // Kludge for Java 7's lack of updateAndGet. Should be safe since entropyBits is thread-local.
      entropyBits.get().set(seedLength * Java8Constants.LONG_BYTES);
    }
  }

  private void initSubclassTransientFields() {
    lock.lock();
    try {
      splittableRandoms = new ThreadLocal<SplittableRandom>() {
        @Override public SplittableRandom initialValue() {
          // Necessary because SplittableRandom.split() isn't itself thread-safe.
          lock.lock();
          try {
            return underlying.split();
          } finally {
            lock.unlock();
          }
        }
      };
      entropyBits = new ThreadLocal<AtomicLong>() {
        @Override public AtomicLong initialValue() {
          return new AtomicLong(SEED_LENGTH_BITS);
        }
      };

      // getSeed() will return the master seed on each thread where setSeed() hasn't yet been called
<<<<<<< HEAD
      seeds = new ThreadLocal<byte[]>() {
        @Override public byte[] initialValue() {
          return seed;
        }
      };
=======
      seeds = ThreadLocal.withInitial(() -> seed.clone());
      seedBuffers = ThreadLocal.withInitial(() -> ByteBuffer.wrap(seeds.get()));
>>>>>>> master
    } finally {
      lock.unlock();
    }
    // WTF Checker Framework? Why is this needed?
  }

  @Override protected SplittableRandom getSplittableRandom() {
    return splittableRandoms.get();
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("splittableRandoms", splittableRandoms);
  }

  /**
   * Not supported, because this class uses a thread-local seed.
   * @param seedGenerator ignored.
   * @throws UnsupportedOperationException always.
   */
  @Override public void setSeedGenerator(@Nullable final SeedGenerator seedGenerator) {
    throw new UnsupportedOperationException("Use ReseedingSplittableRandomAdapter instead");
  }

  @Override public byte[] getSeed() {
    return seeds.get().clone();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(final byte[] seed) {
    if (seed.length != Java8Constants.LONG_BYTES) {
      throw new IllegalArgumentException("SplittableRandomAdapter requires an 8-byte seed");
    }
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (splittableRandoms != null) {
      splittableRandoms.set(new SplittableRandom(seed));
      if (entropyBits != null) {
<<<<<<< HEAD
        creditEntropyForNewSeed(Java8Constants.LONG_BYTES);
=======
        creditEntropyForNewSeed(8);
>>>>>>> master
      }
      if (seeds != null) {
        seedBuffers.get().putLong(0, seed);
      }
    }
  }
}
