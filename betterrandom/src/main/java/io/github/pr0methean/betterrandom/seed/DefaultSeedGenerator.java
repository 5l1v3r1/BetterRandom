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

/**
 * Seed generator that maintains multiple strategies for seed generation and will delegate to the
 * best one available for the current operating environment.
 *
 * @author Daniel Dyer
 * @version $Id: $Id
 */
public enum DefaultSeedGenerator implements SeedGenerator {

  /**
   * Singleton instance.
   */
  DEFAULT_SEED_GENERATOR;

  /**
   * Delegate generators.
   */
  private static final SeedGenerator[] GENERATORS = {
      DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR,
      RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR,
      SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR
  };

  @Override
  public void generateSeed(final byte[] output) throws SeedException {
    for (final SeedGenerator generator : GENERATORS) {
      try {
        generator.generateSeed(output);
        return;
      } catch (final SeedException ignored) {
        // Try the next one
      }
    }
    // This shouldn't happen as at least one the generators should be
    // able to generate a seed.
    throw new SeedException("All available seed generation strategies failed.");
  }

  /**
   * {@inheritDoc}
   * <p>
   * Generates a seed by trying each of the available strategies in turn until one succeeds.  Tries
   * the most suitable strategy first and eventually degrades to the least suitable (but guaranteed
   * to work) strategy.
   */
  @Override
  public byte[] generateSeed(final int length) throws SeedException {
    for (final SeedGenerator generator : GENERATORS) {
      try {
        return generator.generateSeed(length);
      } catch (final SeedException ex) {
        // Ignore and try the next generator...
      }
    }
    // This shouldn't happen as at least one the generators should be
    // able to generate a seed.
    throw new SeedException("All available seed generation strategies failed.");
  }
}
