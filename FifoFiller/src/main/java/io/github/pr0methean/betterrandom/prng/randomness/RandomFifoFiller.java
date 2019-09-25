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
package io.github.pr0methean.betterrandom.prng.randomness;

import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Utility to populate a fifo with input, so it can be fed into a randomness test suite.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
public final class RandomFifoFiller
{
    private RandomFifoFiller()
    {
        // Prevents instantiation.
    }


    /**
     * @param args The first argument is the class name of the RNG, the second
     * is the file to use for output.
     * @throws Exception If there are problems setting up the RNG or writing to
     * the output file.
     */
    public static void main(String[] args) throws Exception
    {
        if (args.length != 2)
        {
            System.out.println("Expected arguments:");
            System.out.println("\t<Fully-qualified RNG class name> <Output file>");
            System.exit(1);
        }
        generateOutputFile(
            Class.forName(args[0])
                .asSubclass(Random.class)
                .getConstructor(SeedGenerator.class)
                .newInstance(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR),
            new File(args[1]));
    }


    /**
     * Populates a file with random numbers as long as it can be written to.
     * Intended for use with a named pipe.
     *
     * @param rng The random number generator to use to generate the data.
     * @param outputFile The file that the random data is written to.
     */
    @SuppressWarnings("InfiniteLoopStatement") public static void generateOutputFile(Random rng,
        File outputFile) {
      try (DataOutputStream dataOutput = new DataOutputStream(
          new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            while (true) {
                dataOutput.writeLong(rng.nextLong());
            }
      } catch (IOException expected) {
            // Broken pipe when Dieharder is finished
        }
    }
}
