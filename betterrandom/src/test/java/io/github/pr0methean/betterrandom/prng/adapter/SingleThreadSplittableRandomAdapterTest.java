package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.testEquivalence;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest extends BaseRandomTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return SingleThreadSplittableRandomAdapter.class;
  }

  @Override @Test(enabled = false) public void testThreadSafety() {
    // No-op because this class isn't thread-safe.
  }

  /**
   * {@inheritDoc} Overridden in subclasses, so that subclassing the test can test the subclasses.
   */
  @Override protected BaseSplittableRandomAdapter createRng() throws SeedException {
    return new SingleThreadSplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final BaseSplittableRandomAdapter adapter = createRng();
    adapter.setSeed(seed);
    return adapter;
  }

  @Test public void testGetSplittableRandom() throws Exception {
    // TODO
  }

  @Override @Test public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = createRng();
    // May change when serialized and deserialized, but deserializing twice should yield same object
    // and deserialization should be idempotent
    final BaseSplittableRandomAdapter adapter2 = CloneViaSerialization.clone(adapter);
    final BaseSplittableRandomAdapter adapter3 = CloneViaSerialization.clone(adapter);
    final BaseSplittableRandomAdapter adapter4 = CloneViaSerialization.clone(adapter2);
    testEquivalence(adapter2, adapter3, 20);
    testEquivalence(adapter2, adapter4, 20);
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(0x0123456789ABCDEFL);
    rng2.setSeed(0x0123456789ABCDEFL);
    assert RandomTestUtils.testEquivalence(rng, rng2, 20)
        : "Output mismatch after reseeding with same seed";
  }

  @Override @Test public void testNullSeed() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testEquals() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testHashCode() {
    // No-op.
  }
}