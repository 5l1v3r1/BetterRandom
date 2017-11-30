package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.Random;
import org.testng.annotations.Test;

public class RandomWrapperAesCounterRandomTest extends AesCounterRandom128Test {

  private static final NamedFunction<Random, Double> SET_WRAPPED = new NamedFunction<>(random -> {
    ((RandomWrapper) random).setWrapped(new AesCounterRandom());
    return 0.0;
  }, "setWrapped");

  @Override public void testThreadSafety() {
    super.testThreadSafety();
    testThreadSafetyVsCrashesOnly(
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }

  @Override @Test(enabled = false) public void testAdvanceForward() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceBackward() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceZero() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom());
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
