package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Random;
import java8.util.function.Function;
import java8.util.function.LongFunction;
import java8.util.function.Supplier;
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

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, new MersenneTwisterRandomColonColonNew());
    params
        .put(Function.class, new RandomColonColonNewForByteArray());
    return params;
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        new Function<byte[], BaseRandom>() {
          @Override public BaseRandom apply(byte[] seed) {
            return new AesCounterRandom(seed);
          }
        }).getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(new LongFunction<Random>() {
      @Override public Random apply(long seed) {
        return new Random(seed);
      }
    }, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR).nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(new MersenneTwisterRandomColonColonNew());
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return createRng();
  }

  protected static class MersenneTwisterRandomColonColonNew
      implements SerializableSupplier<MersenneTwisterRandom> {

    @Override public MersenneTwisterRandom get() {
      return new MersenneTwisterRandom();
    }
  }

  protected static class RandomColonColonNewForByteArray implements LongFunction<Random> {

    @Override public Random apply(long seed) {
      return new Random(seed);
    }
  }
}
