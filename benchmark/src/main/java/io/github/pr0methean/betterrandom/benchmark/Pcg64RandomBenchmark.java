package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class Pcg64RandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new Pcg64Random(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
  }
}
