package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class SplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return new SplittableRandomAdapter(SECURE_RANDOM_SEED_GENERATOR);
  }
}
