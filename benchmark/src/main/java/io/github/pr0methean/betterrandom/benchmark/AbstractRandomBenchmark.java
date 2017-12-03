package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.profile.HotspotThreadProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@EntryPoint
@State(Scope.Benchmark)
abstract class AbstractRandomBenchmark {

  private static final int COLUMNS = 2;
  private static final int ROWS = 50_000;
  @SuppressWarnings("MismatchedReadAndWriteOfArray") private final byte[][] bytes =
      new byte[COLUMNS][ROWS];
  protected Random prng;

  protected AbstractRandomBenchmark() {
  }

  @SuppressWarnings("ObjectAllocationInLoop") public static void main(final String[] args)
      throws RunnerException {
    final ChainedOptionsBuilder options =
        new OptionsBuilder().addProfiler(HotspotThreadProfiler.class)
            .addProfiler(HotspotRuntimeProfiler.class).addProfiler(HotspotMemoryProfiler.class)
            .addProfiler(GCProfiler.class).addProfiler(StackProfiler.class).shouldFailOnError(true)
            .forks(1).resultFormat(ResultFormatType.CSV).detectJvmArgs();
    for (int nThreads = 1; nThreads <= 2; nThreads++) {
      new Runner(
          options.threads(nThreads).output(String.format("%d-thread_bench_results.csv", nThreads))
              .build()).run();
    }
  }

  @Setup(Level.Trial) public void setUp() {
    prng = createPrng();
  }

  @EntryPoint protected abstract Random createPrng() throws SeedException;

  byte innerTestBytesSequential() {
    for (final byte[] column : bytes) {
      prng.nextBytes(column);
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark public byte testBytesSequential() {
    return innerTestBytesSequential();
  }
}
