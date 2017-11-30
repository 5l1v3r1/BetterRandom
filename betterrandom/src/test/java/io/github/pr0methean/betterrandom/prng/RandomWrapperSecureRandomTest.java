package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import org.testng.annotations.Test;

public class RandomWrapperSecureRandomTest extends BaseRandomTest {

  private static final SecureRandom SEED_GEN = new SecureRandom();
  private static final NamedFunction<Random, Double> SET_WRAPPED = new NamedFunction<>(random -> {
    ((RandomWrapper) random).setWrapped(new SecureRandom());
    return 0.0;
  }, "setWrapped");

  private static RandomWrapper createRngInternal() {
    try {
      return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new SecureRandom());
    return params;
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override @Test public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    super.testSeedTooLong();
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override @Test public void testSeedTooShort() throws SeedException {
    super.testSeedTooShort();
  }

  @Override @Test(enabled = false) public void testNullSeed() throws SeedException {
    // No-op.
  }

  /**
   * Only test for crashes, since {@link SecureRandom#setSeed(long)} doesn't completely replace the
   * existing seed.
   */
  @Override public void testSetSeed() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    prng.nextLong();
  }

  @Override @Test(enabled = false) public void testRepeatability() throws SeedException {
    // No-op.
  }

  @Override protected BaseRandom createRng() throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(seed);
    return wrapper;
  }

  /** Assertion-free because SecureRandom isn't necessarily reproducible. */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }
}
