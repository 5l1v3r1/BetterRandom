package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.prng.EntropyBlockingHelper;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

public class EntropyBlockingSplittableRandomAdapter extends SplittableRandomAdapter {
  private static final long serialVersionUID = 4992825526245524633L;
  private transient ThreadLocal<EntropyBlockingHelper> helpers;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final long minimumEntropy;

  public EntropyBlockingSplittableRandomAdapter(long minimumEntropy, SeedGenerator seedGenerator) {
    super(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    initSubclassTransientFields();
  }

  public EntropyBlockingSplittableRandomAdapter(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    initSubclassTransientFields();
    helpers.get().checkMaxOutputAtOnce();
  }

  public EntropyBlockingSplittableRandomAdapter(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    initSubclassTransientFields();
    helpers.get().checkMaxOutputAtOnce();
  }

  private void initSubclassTransientFields() {
    helpers = new ThreadLocal<EntropyBlockingHelper>() {
      @Override public EntropyBlockingHelper initialValue() {
        ThreadLocalFields threadLocalFields =
            EntropyBlockingSplittableRandomAdapter.this.threadLocalFields.get();
        return new EntropyBlockingHelper(minimumEntropy,
            EntropyBlockingSplittableRandomAdapter.this.sameThreadSeedGen,
            EntropyBlockingSplittableRandomAdapter.this, threadLocalFields.entropyBits,
            new ReentrantLock());
      }
    };
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  @Override protected void debitEntropy(long bits) {
    helpers.get().debitEntropy(bits);
  }

  @Override public void setSeed(long seed) {
    if (helpers == null) {
      super.setSeed(seed);
      return;
    }
    lock.lock();
    try {
      super.setSeed(seed);
      helpers.get().onSeedingStateChanged(true);
    } finally {
      lock.unlock();
    }
  }
}
