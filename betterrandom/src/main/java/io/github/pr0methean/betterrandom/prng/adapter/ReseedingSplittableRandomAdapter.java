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
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

/**
 * Like {@link SplittableRandomAdapter}, but uses a {@link RandomSeederThread} to replace each
 * thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not more
 * frequently than it is being used.
 * @author Chris Hennick
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  @SuppressWarnings("StaticCollection")
  private static final Map<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  @SuppressWarnings(
      {"ThreadLocalNotStaticFinal", "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(Long.BYTES));
    this.seedGenerator.set(seedGenerator);
    initSubclassTransientFields();
  }

  /**
   * Returns the instance backed by the {@link DefaultSeedGenerator}.
   * @return The instance backed by the {@link DefaultSeedGenerator}.
   * @throws SeedException if the {@link DefaultSeedGenerator} throws one while generating the
   *     initial seed.
   */
  @SuppressWarnings("NonThreadSafeLazyInitialization")
  public static ReseedingSplittableRandomAdapter getDefaultInstance() throws SeedException {
    return getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /**
   * Returns the instance backed by the given {@link SeedGenerator}.
   * @param seedGenerator The seed generator the returned adapter is to use.
   * @return the ReseedingSplittableRandomAdapter backed by {@code seedGenerator}.
   * @throws SeedException if {@code seedGenerator} throws one while generating the initial
   *     seed.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(final SeedGenerator seedGenerator)
      throws SeedException {
    synchronized (INSTANCES) {
      return INSTANCES.computeIfAbsent(seedGenerator, ReseedingSplittableRandomAdapter::new);
    }
  }

  @Override public void setSeedGenerator(SeedGenerator seedGenerator) {
    throw new UnsupportedOperationException(
        "ReseedingSplittableRandomAdapter's binding to RandomSeederThread is immutable");
  }

  @Override protected boolean useParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  private ReseedingSplittableRandomAdapter readResolve() {
    return getInstance(seedGenerator.get());
  }

  private void initSubclassTransientFields() {
    if (threadLocal == null) {
      threadLocal = ThreadLocal
          .withInitial(() -> new SingleThreadSplittableRandomAdapter(seedGenerator.get()));
    }
  }

  @Override protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread = threadLocal.get();
    RandomSeederThread.add(seedGenerator.get(), adapterForThread);
    return adapterForThread.getSplittableRandom();
  }

  @Override public boolean equals(@Nullable final Object o) {
    return (this == o) || ((o instanceof ReseedingSplittableRandomAdapter) && seedGenerator.get()
        .equals(((ReseedingSplittableRandomAdapter) o).seedGenerator.get()));
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    this.seed = seed.clone();
    if (entropyBits == null) {
      entropyBits = new AtomicLong(0);
    }
  }

  @Override public int hashCode() {
    return seedGenerator.get().hashCode() + 1;
  }

  @Override public String toString() {
    return "ReseedingSplittableRandomAdapter using " + seedGenerator;
  }
}
