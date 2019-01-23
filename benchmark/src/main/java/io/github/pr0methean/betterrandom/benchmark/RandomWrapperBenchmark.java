package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.concurrent.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class RandomWrapperBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new RandomWrapper(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
  }
}
