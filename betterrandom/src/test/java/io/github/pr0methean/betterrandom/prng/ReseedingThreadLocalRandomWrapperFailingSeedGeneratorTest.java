package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:FailingSeedGenerator")
public class ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest
    extends ReseedingThreadLocalRandomWrapperTest {

  public ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest() {
    pcgSupplier = (Serializable & Supplier<BaseRandom>)
        () -> new Pcg64Random(semiFakeSeedGenerator);
  }

  @Override @Test(enabled = false) public void testWrapLegacy() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextLong() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSerializable() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSeedTooLong() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testThreadSafety() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    // No-op.
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.EXACT;
  }

  @Override @Test(enabled = false) public void testReseeding() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedZero() {
    // No-op.
  }

  @Override protected SeedGenerator getTestSeedGenerator() {
    return FailingSeedGenerator.FAILING_SEED_GENERATOR;
  }
}
