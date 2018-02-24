package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
import static io.github.pr0methean.betterrandom.seed.RandomSeederThread.stopAllEmpty;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class RandomSeederThreadTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int TEST_OUTPUT_SIZE = 20;

  @Test public void testAddRemoveAndIsEmpty() throws Exception {
    final Random prng = new Random(TEST_SEED);
    final byte[] bytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final SeedGenerator seedGenerator = new FakeSeedGenerator();
    assertTrue(RandomSeederThread.isEmpty(seedGenerator));
    RandomSeederThread.add(seedGenerator, prng);
    assertFalse(RandomSeederThread.isEmpty(seedGenerator));
    Thread.sleep(1000);
    assertFalse(RandomSeederThread.isEmpty(seedGenerator));
    RandomSeederThread.remove(seedGenerator, prng);
    assertTrue(RandomSeederThread.isEmpty(seedGenerator));
    final byte[] bytesWithNewSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithNewSeed);
    assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed));
  }

  @Test public void testStopIfEmpty() throws Exception {
    final SeedGenerator seedGenerator = new FakeSeedGenerator();
    final Random prng = new Random();
    RandomSeederThread.add(seedGenerator, prng);
    RandomSeederThread.stopIfEmpty(seedGenerator);
    assertTrue(RandomSeederThread.hasInstance(seedGenerator));
    RandomSeederThread.remove(seedGenerator, prng);
    RandomSeederThread.stopIfEmpty(seedGenerator);
    Thread.sleep(1000);
    assertFalse(RandomSeederThread.hasInstance(seedGenerator));
  }

  @Test public void testStopAllEmpty() throws Exception {
    final SeedGenerator neverAddedTo = new FakeSeedGenerator();
    final SeedGenerator addedToAndRemoved = new FakeSeedGenerator();
    final SeedGenerator addedToAndLeft = new FakeSeedGenerator();
    final Random addedAndRemoved = new Random();
    final Random addedAndLeft = new Random();
    RandomSeederThread.add(addedToAndRemoved, addedAndRemoved);
    RandomSeederThread.remove(addedToAndRemoved, addedAndRemoved);
    RandomSeederThread.add(addedToAndLeft, addedAndLeft);
    assertFalse(RandomSeederThread.hasInstance(neverAddedTo));
    assertTrue(RandomSeederThread.hasInstance(addedToAndRemoved));
    assertTrue(RandomSeederThread.hasInstance(addedToAndLeft));
    stopAllEmpty();
    Thread.sleep(500);
    assertFalse(RandomSeederThread.hasInstance(neverAddedTo));
    assertFalse(RandomSeederThread.hasInstance(addedToAndRemoved));
    assertTrue(RandomSeederThread.hasInstance(addedToAndLeft));
    addedAndLeft.nextInt(); // prevent GC before this point
  }

  @Test public void testSetDefaultPriority() {
    stopAllEmpty();
    assertFalse(RandomSeederThread.hasInstance(DEFAULT_SEED_GENERATOR));
    RandomSeederThread.setDefaultPriority(7);
    final Random prng = new Random();
    RandomSeederThread.add(DEFAULT_SEED_GENERATOR, prng);
    boolean threadFound = false;
    Thread[] threads = new Thread[10 + Thread.activeCount()];
    int nThreads = Thread.enumerate(threads);
    for (int i = 0; i < nThreads; i++) {
      if ((threads[i] instanceof RandomSeederThread)
          && threads[i].getName().equals("RandomSeederThread for " + DEFAULT_SEED_GENERATOR)) {
        assertEquals(threads[i].getPriority(), 7);
        threadFound = true;
        break;
      }
    }
    assertTrue(threadFound, "Couldn't find the seeder thread!");
    prng.nextInt(); // prevent GC before this point
  }

  @Test public void testSetPriority() {
    final Random prng = new Random();
    RandomSeederThread.add(DEFAULT_SEED_GENERATOR, prng);
    RandomSeederThread.setPriority(DEFAULT_SEED_GENERATOR, 7);
    boolean threadFound = false;
    final Thread[] threads = new Thread[10 + Thread.activeCount()];
    final int nThreads = Thread.enumerate(threads);
    for (int i = 0; i < nThreads; i++) {
      if ((threads[i] instanceof RandomSeederThread)
          && threads[i].getName().equals("RandomSeederThread for " + DEFAULT_SEED_GENERATOR)) {
        assertEquals(threads[i].getPriority(), 7);
        threadFound = true;
        break;
      }
    }
    assertTrue(threadFound, "Couldn't find the seeder thread!");
    prng.nextInt(); // prevent GC before this point
  }

  @AfterMethod public void tearDown() {
    System.gc();
    stopAllEmpty();
  }
}