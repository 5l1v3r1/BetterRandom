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
package io.github.pr0methean.betterrandom.seed;

import java.security.SecureRandom;

/**
 * <p>{@link SeedGenerator} implementation that uses Java's bundled {@link SecureRandom} RNG to
 * generate random seed data.</p> <p>The advantage of using SecureRandom for seeding but not as the
 * primary RNG is that we can use it to seed RNGs that are much faster than SecureRandom.</p>
 * <p>This is the only seeding strategy that is guaranteed to work on all platforms and therefore
 * is
 * provided as a fall-back option should none of the other provided {@link SeedGenerator}
 * implementations be usable.</p> <p>On Oracle and OpenJDK, SecureRandom uses <a
 * href="http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/src/share/classes/sun/security/provider/SeedGenerator.java">{@code
 * sun.security.provider.SeedGenerator}</a>; when {@code /dev/random} isn't available, that
 * SeedGenerator class in turn uses the timing of newly-launched threads as a source of randomness,
 * relying on the unpredictable interactions between different configurations of hardware and
 * software and their workloads.</p>
 * @author Daniel Dyer
 */
public enum SecureRandomSeedGenerator implements SeedGenerator {

  SECURE_RANDOM_SEED_GENERATOR;

  /** The {@link SecureRandom} that generates the seeds. */
  private static final SecureRandom SOURCE = new SecureRandom();

  /**
   * Generates and returns a seed value for a random number generator as a new array.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  @Override public byte[] generateSeed(final int length) {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    return SOURCE.generateSeed(length);
  }

  @Override public void generateSeed(final byte[] output) {
    System.arraycopy(SOURCE.generateSeed(output.length), 0, output, 0, output.length);
  }

  @Override public String toString() {
    return "java.security.SecureRandom";
  }

  /**
   * Returns true if we cannot determine quickly (i.e. without I/O calls) that this SeedGenerator
   * would throw a {@link SeedException} if {@link #generateSeed(int)} or {@link
   * #generateSeed(byte[])} were being called right now.
   * @return true if this SeedGenerator will get as far as an I/O call or other slow operation in
   *     attempting to generate a seed immediately.
   */
  @Override public boolean isWorthTrying() {
    return true;
  }
}
