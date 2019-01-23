package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.concurrent.ThreadLocalRandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

public class ThreadLocalRandomWrapperAesCounterRandom128Benchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return new ThreadLocalRandomWrapper(16, SECURE_RANDOM_SEED_GENERATOR,
        AesCounterRandom::new);
  }
}
