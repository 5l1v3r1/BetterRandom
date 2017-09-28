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
package io.github.pr0methean.betterrandom;

import io.github.pr0methean.betterrandom.util.EntryPoint;

/**
 * Deterministic random number generators are repeatable, which can prove useful for testing and
 * validation.  This interface defines an operation to return the seed data from a repeatable RNG.
 * This seed value can then be reused to create a random source with identical output.
 *
 * @author Daniel Dyer
 * @version $Id: $Id
 */
public interface RepeatableRandom {

  /**
   * Returns the seed.
   *
   * @return The seed data used to initialise this pseudo-random number generator.
   */
  @EntryPoint
  byte[] getSeed();
}
