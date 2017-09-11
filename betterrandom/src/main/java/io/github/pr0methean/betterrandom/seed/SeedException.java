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
 * Exception thrown by {@link io.github.pr0methean.betterrandom.seed.SeedGenerator} implementations
 * when they are unable to generate a new seed for an RNG.
 *
 * @author Daniel Dyer
 * @version $Id: $Id
 */
public class SeedException extends Exception {

  private static final long serialVersionUID = -6151013676983010168L;

  /**
   * <p>Constructor for SeedException.</p>
   *
   * @param message Details of the problem.
   */
  public SeedException(final String message) {
    super(message);
  }

  /**
   * <p>Constructor for SeedException.</p>
   *
   * @param message Details of the problem.
   * @param cause The root cause of the problem.
   */
  public SeedException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
