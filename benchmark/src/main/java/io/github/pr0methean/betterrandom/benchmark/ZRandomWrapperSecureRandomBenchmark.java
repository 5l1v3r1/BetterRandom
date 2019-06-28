package io.github.pr0methean.betterrandom.benchmark;

import java.security.SecureRandom;
import java.util.Random;

public class ZRandomWrapperSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws Exception {
    return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
  }
}
