package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * A version of {@link io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter} that
 * uses a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread} to replace each
 * seederThread's {@link java.util.SplittableRandom} with a reseeded one as frequently as possible,
 * but not more frequently than it is being used.
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private static final Map<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  protected final SeedGenerator seedGenerator;
  protected RandomSeederThread seederThread; // Hide inherited, because not nullable for us
  @SuppressWarnings({"ThreadLocalNotStaticFinal",
      "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   *
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
    this.seedGenerator = seedGenerator;
    initSubclassTransientFields();
  }

  /**
   * <p>getDefaultInstance.</p>
   *
   * @return a {@link io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter}
   *     object.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  @SuppressWarnings("NonThreadSafeLazyInitialization")
  public static ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    return getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /**
   * <p>getInstance.</p>
   *
   * @param seedGenerator a {@link io.github.pr0methean.betterrandom.seed.SeedGenerator}
   *     object.
   * @return a {@link io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter}
   *     object.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(final SeedGenerator seedGenerator)
      throws SeedException {
    synchronized (INSTANCES) {
      try {
        return INSTANCES.computeIfAbsent(seedGenerator, seedGen -> {
          try {
            return new ReseedingSplittableRandomAdapter(seedGen);
          } catch (final SeedException e) {
            throw new RuntimeException(e);
          }
        });
      } catch (final RuntimeException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof SeedException) {
          throw (SeedException) cause;
        } else {
          throw e;
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seedGenerator != null : "@AssumeAssertion(nullness)";
    initSubclassTransientFields();
  }

  private ReseedingSplittableRandomAdapter readResolve() throws IOException {
    try {
      return getInstance(seedGenerator);
    } catch (final SeedException e) {
      throw new IOException(e);
    }
  }

  @EnsuresNonNull({"threadLocal", "seederThread"})
  @RequiresNonNull("seedGenerator")
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)ReseedingSplittableRandomAdapter this) {
    if (threadLocal == null) {
      threadLocal = ThreadLocal.withInitial(() -> {
        try {
          return new SingleThreadSplittableRandomAdapter(seedGenerator);
        } catch (final SeedException e) {
          throw new RuntimeException(e);
        }
      });
    }
    seederThread = RandomSeederThread.getInstance(seedGenerator);
  }

  /** {@inheritDoc} */
  @Override
  protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread = threadLocal.get();
    seederThread.add(adapterForThread);
    return adapterForThread.getSplittableRandom();
  }

  /** {@inheritDoc} */
  @Override
  protected void finalize() {
    seederThread.stopIfEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final @Nullable Object o) {
    return this == o
        || (o instanceof ReseedingSplittableRandomAdapter
        && seedGenerator.equals(((ReseedingSplittableRandomAdapter) o).seedGenerator));
  }

  /** {@inheritDoc} */
  @Override
  public void setSeed(@UnknownInitialization ReseedingSplittableRandomAdapter this,
      final long seed) {
    // No-op.
  }

  /** {@inheritDoc} */
  @Override
  protected void setSeedInternal(@UnknownInitialization ReseedingSplittableRandomAdapter this,
      final byte[] seed) {
    this.seed = seed.clone();
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return seedGenerator.hashCode() + 1;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "ReseedingSplittableRandomAdapter using " + seedGenerator;
  }
}
