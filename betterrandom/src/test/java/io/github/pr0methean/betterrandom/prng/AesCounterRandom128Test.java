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
package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 * @author Daniel Dyer
 */
public class AesCounterRandom128Test extends SeekableRandomTest {

  @SuppressWarnings("ObjectAllocationInLoop") @Override @Test(timeOut = 30000)
  public void testSetSeed() throws SeedException {
    // can't use a real SeedGenerator since we need longs, so use a Random
    final Random masterRNG = new Random();
    final long[] seeds =
        {masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong()};
    final long otherSeed = masterRNG.nextLong();
    final AesCounterRandom[] rngs = {new AesCounterRandom(16), new AesCounterRandom(16)};
    for (int i = 0; i < 2; i++) {
      for (final long seed : seeds) {
        final byte[] originalSeed = rngs[i].getSeed();
        assertTrue(originalSeed.length >= 16);
        final AesCounterRandom rngReseeded = new AesCounterRandom(originalSeed);
        final AesCounterRandom rngReseededOther = new AesCounterRandom(originalSeed);
        rngReseeded.setSeed(seed);
        rngReseededOther.setSeed(otherSeed);
        assert !(rngs[i].equals(rngReseeded));
        assert !(rngReseededOther.equals(rngReseeded));
        assert rngs[i].nextLong() != rngReseeded.nextLong() : "setSeed had no effect";
        rngs[i] = rngReseeded;
      }
    }
    assert rngs[0].nextLong() != rngs[1].nextLong() : "RNGs converged after 4 setSeed calls";
  }

  @Test(timeOut = 15000) public void testMaxSeedLengthOk() {
    assert AesCounterRandom.getMaxKeyLengthBytes() >= 16 : "Should allow a 16-byte key";
    assert AesCounterRandom.getMaxKeyLengthBytes() <= 32
        : "Shouldn't allow a key longer than 32 bytes";
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return AesCounterRandom.class;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new AesCounterRandom(16);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new AesCounterRandom(seed);
  }
}
