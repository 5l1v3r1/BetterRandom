package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  @Override public void testSerializable()
      throws IOException, ClassNotFoundException, SeedException {
    // May change after serialization, so test only that it still works at all afterward
    CloneViaSerialization.clone(createRng()).nextInt();
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    createRng().setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(17));
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng().setSeed(new byte[]{1, 2, 3});
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng().setSeed(null);
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testRepeatability() throws SeedException {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
  }

  /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeedGenerator(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /** Assertion-free because thread-local. */
  @Override @Test public void testThreadSafety() {
    ConcurrentSkipListSet<Long> sequentialLongs = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Long> parallelLongs = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialLongs, parallelLongs, Random::nextLong);
    ConcurrentSkipListSet<Double> sequentialDoubles = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Double> parallelDoubles = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextDouble);
    sequentialDoubles.clear();
    parallelDoubles.clear();
    runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextGaussian);
    ConcurrentSkipListSet<Integer> sequentialInts = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Integer> parallelInts = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialInts, parallelInts, Random::nextInt);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, (Supplier<MersenneTwisterRandom>) MersenneTwisterRandom::new);
    params
        .put(Function.class, (Function<byte[], MersenneTwisterRandom>) MersenneTwisterRandom::new);
    return params;
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        AesCounterRandom::new).getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)
        .nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(
        (Serializable & Supplier<BaseRandom>) MersenneTwisterRandom::new);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return createRng();
  }
}
