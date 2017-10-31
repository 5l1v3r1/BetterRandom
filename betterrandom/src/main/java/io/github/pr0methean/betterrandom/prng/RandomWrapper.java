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
package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.security.SecureRandom;
import java.util.Random;
import javax.annotation.Nullable;

/**
 * <p>Wraps any {@link Random} as a {@link RepeatableRandom} and {@link ByteArrayReseedableRandom}.
 * Can be used to encapsulate away a change of implementation in midstream.</p>
 * @author Chris Hennick
 */
public class RandomWrapper extends BaseRandom {

  @Override public String toString() {
    return String.format("RandomWrapper (currently around %s)", wrapped);
  }

  protected static final byte[] DUMMY_SEED = new byte[8];
  private static final long serialVersionUID = -6526304552538799385L;
  private volatile Random wrapped;
  private boolean unknownSeed = true;
  private boolean haveParallelStreams;

  /**
   * Wraps a {@link Random} and seeds it using the default seeding strategy.
   * @throws SeedException if any.
   */
  public RandomWrapper() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(Long.BYTES));
  }

  /**
   * Wraps a {@link Random} and seeds it using the provided seedArray generation strategy.
   * @param seedGenerator The seedArray generation strategy that will provide the seedArray
   *     value for this RNG.
   * @throws SeedException If there is a problem generating a seedArray.
   */
  @EntryPoint public RandomWrapper(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator, Long.BYTES);
    wrapped = new Random(BinaryUtils.convertBytesToLong(seed));
    unknownSeed = false;
    haveParallelStreams = wrapped.longs().isParallel();
  }

  /**
   * Wraps a new {@link Random} seeded it with the specified seed data.
   * @param seed 8 bytes of seed data used to initialise the RNG.
   */
  public RandomWrapper(final byte[] seed) {
    super(seed);
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException(
          "RandomWrapper requires an 8-byte seed when defaulting to java.util.Random");
    }
    wrapped = new Random(BinaryUtils.convertBytesToLong(seed));
    unknownSeed = false;
    haveParallelStreams = wrapped.longs().isParallel();
  }

  /**
   * Wraps a new {@link Random} seeded with the specified seed.
   * @param seed seed used to initialise the {@link Random}.
   */
  @EntryPoint public RandomWrapper(final long seed) {
    super(seed);
    wrapped = new Random(seed);
    unknownSeed = false;
    haveParallelStreams = wrapped.longs().isParallel();
  }

  /**
   * Creates an instance wrapping the given {@link Random}.
   * @param wrapped The {@link Random} to wrap.
   */
  @EntryPoint public RandomWrapper(final Random wrapped) {
    super(getSeedOrDummy(wrapped)); // We won't know the wrapped PRNG's seed
    unknownSeed = !(wrapped instanceof RepeatableRandom);
    readEntropyOfWrapped(wrapped);
    this.wrapped = wrapped;
    haveParallelStreams = wrapped.longs().isParallel();
  }

  private static byte[] getSeedOrDummy(final Random wrapped) {
    return (wrapped instanceof RepeatableRandom) ? ((RepeatableRandom) wrapped).getSeed()
        : DUMMY_SEED;
  }

  @Override protected boolean useParallelStreams() {
    return haveParallelStreams;
  }

  @Override protected int next(final int bits) {
    return (bits >= 32) ? getWrapped().nextInt() : getWrapped().nextInt(1 << bits);
  }

  /**
   * Returns the {@link Random} instance this RandomWrapper is currently wrapping.
   * @return The wrapped {@link Random}.
   */
  @EntryPoint public Random getWrapped() {
    lock.lock();
    try {
      return wrapped;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Replaces the underlying {@link Random} instance with the given one on subsequent calls.
   * @param wrapped The new {@link Random} instance to wrap.
   */
  @EntryPoint public void setWrapped(final Random wrapped) {
    lock.lock();
    try {
      this.wrapped = wrapped;
      readEntropyOfWrapped(wrapped);
      seed = getSeedOrDummy(wrapped);
      unknownSeed = !(wrapped instanceof RepeatableRandom);
      haveParallelStreams = wrapped.longs().isParallel();
    } finally {
      lock.unlock();
    }
  }

  private void readEntropyOfWrapped(final Random wrapped) {
    entropyBits.set((wrapped instanceof EntropyCountingRandom) ? ((EntropyCountingRandom) wrapped)
        .getEntropyBits()
        : ((wrapped instanceof RepeatableRandom) ? (((RepeatableRandom) wrapped).getSeed().length
            * (long) (Byte.SIZE)) : Long.SIZE));
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("wrapped", wrapped);
  }

  /**
   * Returns the wrapped PRNG's seed, if we know it. When this RandomWrapper is wrapping a passed-in
   * {@link Random} that's not a {@link RepeatableRandom}, we won't know the seed until the next
   * {@link #setSeed(byte[])} or {@link #setSeed(long)} call lets us set it ourselves, and so an
   * {@link UnsupportedOperationException} will be thrown until then.
   * @throws UnsupportedOperationException if this RandomWrapper doesn't know the wrapped PRNG's
   *     seed.
   */
  @Override public byte[] getSeed() {
    if (unknownSeed) {
      throw new UnsupportedOperationException();
    }
    return super.getSeed();
  }

  /**
   * Delegates to one of {@link ByteArrayReseedableRandom#setSeed(byte[])}, {@link
   * SecureRandom#setSeed(byte[])} or {@link Random#setSeed(long)}.
   * @param seed The new seed.
   */
  @SuppressWarnings("LockAcquiredButNotSafelyReleased") @Override protected void setSeedInternal(
      final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    boolean locked = false;
    if (lock != null) {
      lock.lock();
      locked = true;
    }
    try {
      if ((this.seed == null) || (this.seed.length != seed.length)) {
        this.seed = new byte[seed.length];
      }
      super.setSeedInternal(seed);
      if (wrapped != null) {
        @Nullable ByteArrayReseedableRandom asByteArrayReseedable = null;
        if (wrapped instanceof ByteArrayReseedableRandom) {
          asByteArrayReseedable = (ByteArrayReseedableRandom) wrapped;
          if (asByteArrayReseedable.preferSeedWithLong() && (seed.length == Long.BYTES)) {
            asByteArrayReseedable = null;
          }
        } else if (wrapped instanceof SecureRandom) {
          // Special handling, since SecureRandom isn't ByteArrayReseedableRandom but does have
          // setSeed(byte[])
          ((SecureRandom) wrapped).setSeed(seed);
          unknownSeed = false;
          return;
        } else if (seed.length != Long.BYTES) {
          throw new IllegalArgumentException(
              "RandomWrapper requires an 8-byte seed when not wrapping a ByteArrayReseedableRandom");
        }
        if (asByteArrayReseedable != null) {
          asByteArrayReseedable.setSeed(seed);
          unknownSeed = false;
        } else {
          wrapped.setSeed(BinaryUtils.convertBytesToLong(seed));
          unknownSeed = false;
        }
      }
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  @Override public boolean preferSeedWithLong() {
    final Random currentWrapped = getWrapped();
    return !(currentWrapped instanceof ByteArrayReseedableRandom)
        || ((ByteArrayReseedableRandom) currentWrapped).preferSeedWithLong();
  }

  /** */
  @SuppressWarnings("LockAcquiredButNotSafelyReleased") @Override public int getNewSeedLength(
      ) {
    boolean locked = false;
    if (lock != null) {
      lock.lock();
      locked = true;
    }
    try {
      return (wrapped instanceof ByteArrayReseedableRandom) ? ((ByteArrayReseedableRandom) wrapped)
          .getNewSeedLength() : Long.BYTES;
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  @Override public void nextBytes(final byte[] bytes) {
    getWrapped().nextBytes(bytes);
    recordEntropySpent(bytes.length * (long) (Byte.SIZE));
  }

  @Override public int nextInt() {
    final int result = getWrapped().nextInt();
    recordEntropySpent(Integer.SIZE);
    return result;
  }

  @Override public int nextInt(final int bound) {
    final int result = getWrapped().nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return result;
  }

  @Override protected long nextLongNoEntropyDebit() {
    return getWrapped().nextLong();
  }

  @Override public boolean nextBoolean() {
    final boolean result = getWrapped().nextBoolean();
    recordEntropySpent(1);
    return result;
  }

  @Override public float nextFloat() {
    final float result = getWrapped().nextFloat();
    recordEntropySpent(ENTROPY_OF_FLOAT);
    return result;
  }

  @Override public double nextDouble() {
    final double result = getWrapped().nextDouble();
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return result;
  }

  @Override public double nextGaussian() {
    final double result = getWrapped().nextGaussian();

    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);

    return result;
  }
}
