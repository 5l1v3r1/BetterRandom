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
package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.adapter.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the JDK RNG.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "RandomWrapper")
public class RandomWrapperRandomTest extends BaseRandomTest {

  private static final NamedFunction<Random, Double> SET_WRAPPED = new NamedFunction<>(random -> {
    ((RandomWrapper) random).setWrapped(new Random());
    return 0.0;
  }, "setWrapped");

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new Random());
    return params;
  }

  /**
   * Assertion-free with respect to the long/double methods because, contrary to its contract to be
   * thread-safe, {@link Random#nextLong()} is not transactional. Rather, it uses two calls to
   * {@link Random#next(int)} that can interleave with calls from other threads.
   */
  @Override public void testThreadSafety() {
    testThreadSafety(ImmutableList.of(NEXT_INT), Collections.emptyList());
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(0x0123456789ABCDEFL);
    rng2.setSeed(0x0123456789ABCDEFL);
    RandomTestUtils.assertEquivalent(rng, rng2, 20,
        "Output mismatch after reseeding with same seed");
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Override @Test(timeOut = 30_000) public void testRepeatability() throws SeedException {
    // Create an RNG using the default seeding strategy.
    final RandomWrapper rng = new RandomWrapper(getTestSeedGenerator());
    // Create second RNG using same seed.
    final RandomWrapper duplicateRNG = new RandomWrapper(rng.getSeed());
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 200,
        "Generated sequences do not match.");
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new RandomWrapper(getTestSeedGenerator());
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(seed);
  }
}
