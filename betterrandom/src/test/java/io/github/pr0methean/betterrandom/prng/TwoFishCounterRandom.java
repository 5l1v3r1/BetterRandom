package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic;
import java.security.MessageDigest;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jcajce.provider.digest.SHA3;

/**
 * A second subclass of {@link CipherCounterRandom}, used to test that abstract class for
 * AES-specific behavior (since it was split off as a parent of {@link AesCounterRandom}).
 */
public class TwoFishCounterRandom extends CipherCounterRandom {
  private static final int LARGE_KEY_LENGTH = 32;
  private static final int SMALL_KEY_LENGTH = 16;
  @SuppressWarnings("CanBeFinal") private static int MAX_KEY_LENGTH_BYTES = LARGE_KEY_LENGTH;

  // WARNING: Don't initialize any instance fields at declaration; they may be initialized too late!
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient TwofishEngine cipher;

  public TwoFishCounterRandom(byte[] seed) {
    super(seed);
  }

  @Override
  public int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
  }

  @Override
  protected int getKeyLength(int inputLength) {
    return (inputLength > MAX_KEY_LENGTH_BYTES) ? MAX_KEY_LENGTH_BYTES
        : ((inputLength >= 24) ? 24 : 16);
  }

  @Override
  protected int getMinSeedLength() {
    return SMALL_KEY_LENGTH;
  }

  @Override
  public int getBlocksAtOnce() {
    // FIXME: Some tests fail when this is changed.
    return 1;
  }

  @Override
  protected MessageDigest createHash() {
    return new SHA3.Digest256();
  }

  @Override
  protected void createCipher() {
    lock.lock();
    try {
      cipher = new TwofishEngine();
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void setKey(byte[] key) {
    cipher.init(true, new KeyParameter(key));
  }

  @Override public MoreObjects.ToStringHelper addSubclassFields(final MoreObjects.ToStringHelper original) {
    return original.add("counter", BinaryUtils.convertBytesToHexString(counter))
        .add("cipher", cipher)
        .add("index", index);
  }

  // FIXME
  @Override
  public void advance(final long delta) {
    if (delta == 0) {
      return;
    }
    final long intsPerBlock = COUNTER_SIZE_BYTES / Integer.BYTES;
    long blocksDelta = delta / intsPerBlock;
    final int deltaWithinBlock = (int) (delta % intsPerBlock) * Integer.BYTES;
    lock.lock();
    try {
      int newIndex = index + deltaWithinBlock;
      if (newIndex >= COUNTER_SIZE_BYTES) {
        newIndex -= COUNTER_SIZE_BYTES;
        blocksDelta++;
      }
      if (newIndex < 0) {
        newIndex += COUNTER_SIZE_BYTES;
        blocksDelta--;
      }
      blocksDelta -= getBlocksAtOnce(); // Compensate for the increment during nextBlock() below
      Byte16ArrayArithmetic.addInto(counter, blocksDelta, addendDigits);
      nextBlock();
      index = newIndex;
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void doCipher(byte[] input, byte[] output) {
    cipher.reset();
    cipher.processBlock(input, 0, output, 0);
  }
}
