package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertEquivalent;
import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;
import static org.testng.Assert.assertEquals;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;

public class EntropyBlockingReseedingSplittableRandomAdapterNullTest
    extends EntropyBlockingReseedingSplittableRandomAdapterTest {
  @Override protected EntropyBlockingReseedingSplittableRandomAdapter createRng()
      throws SeedException {
    return new EntropyBlockingReseedingSplittableRandomAdapter(getTestSeedGenerator(), null,
        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
  }

  private EntropyBlockingReseedingSplittableRandomAdapter createRngLargeEntropyLimit() {
    return new EntropyBlockingReseedingSplittableRandomAdapter(getTestSeedGenerator(), null,
        EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY);
  }

  @Override public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRngLargeEntropyLimit();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRngLargeEntropyLimit();
    duplicateRNG.setSeed(TEST_SEED);
    assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match");
  }

  @Override public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter =
        new EntropyBlockingReseedingSplittableRandomAdapter(new SimpleRandomSeeder(DEFAULT_INSTANCE),
            EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
    final BaseSplittableRandomAdapter clone = SerializableTester.reserialize(adapter);
    assertEquals(adapter, clone, "Unequal after serialization round-trip");
  }

  @Override public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(null);
  }
}
