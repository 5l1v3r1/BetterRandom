package io.github.pr0methean.betterrandom.prng.concurrent;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandomTest;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Random;
import org.testng.annotations.Test;

import static org.testng.Assert.assertSame;

@Test(testName = "RandomWrapper:MersenneTwisterRandom")
public class RandomWrapperMersenneTwisterRandomTest extends MersenneTwisterRandomTest {

  private final NamedFunction<Random, Double> setWrapped;

  public RandomWrapperMersenneTwisterRandomTest() {
    final SeedGenerator seedGenerator = getTestSeedGenerator();
    setWrapped = new NamedFunction<>(random -> {
      ((RandomWrapper) random).setWrapped(new MersenneTwisterRandom(seedGenerator));
      return 0.0;
    }, "setWrapped");
  }

  @Override public void testThreadSafety() {
    super.testThreadSafety();
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, setWrapped));
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors()
      throws SeedException {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom(seed));
  }

  // FIXME: This test takes too long!
  @Override @Test(timeOut = 120_000)
  public void testSerializable() throws SeedException {
    super.testSerializable();
  }

  @Override @Test(retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testReseeding() throws SeedException {
    super.testReseeding();
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), MersenneTwisterRandom.class);
  }
}
