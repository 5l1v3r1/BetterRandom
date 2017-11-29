package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java8.util.function.Function;
import java8.util.function.LongFunction;
import java8.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Wraps a {@link ThreadLocal}&lt;{@link BaseRandom}&gt; in order to provide concurrency that most
 * implementations of {@link BaseRandom} can't implement naturally.
 */
public class ThreadLocalRandomWrapper extends RandomWrapper {

  private static final long serialVersionUID = 1199235201518562359L;
  protected final Supplier<? extends BaseRandom> initializer;
  @Nullable private final Integer explicitSeedSize;
  protected transient ThreadLocal<BaseRandom> threadLocal;

  /**
   * Wraps the given {@link Supplier}. This ThreadLocalRandomWrapper will be serializable if the
   * {@link Supplier} is serializable.
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   */
  public ThreadLocalRandomWrapper(final Supplier<? extends BaseRandom> initializer)
      throws SeedException {
    super(0);
    this.initializer = initializer;
    initSubclassTransientFields();
    explicitSeedSize = null;
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ThreadLocalRandomWrapper will be serializable if the {@link Function} is serializable.
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed.
   *     Probably a constructor reference.
   */
  public ThreadLocalRandomWrapper(final int seedSize, final SeedGenerator seedGenerator,
      final Function<byte[], ? extends BaseRandom> creator) throws SeedException {
    super(0);
    explicitSeedSize = seedSize;
    initializer = new SerializableSupplier<BaseRandom>() {
      @Override public BaseRandom get() {
        return creator.apply(seedGenerator.generateSeed(seedSize));
      }
    };
    initSubclassTransientFields();
  }

  /**
   * Uses this class and {@link RandomWrapper} to decorate any implementation of {@link Random} that
   * can be constructed from a {@code long} seed into a fully-concurrent one.
   * @param legacyCreator a function that provides the {@link Random} that underlies the
   *     returned wrapper on each thread, taking a seed as input.
   * @param seedGenerator the seed generator whose output will be fed to {@code legacyCreator}.
   * @return a ThreadLocalRandomWrapper decorating instances created by {@code legacyCreator}.
   */
  public static ThreadLocalRandomWrapper wrapLegacy(final LongFunction<Random> legacyCreator,
      final SeedGenerator seedGenerator) {
    return new ThreadLocalRandomWrapper(Java8Constants.LONG_BYTES, seedGenerator,
        wrapLongCreatorAsByteArrayCreator(legacyCreator));
  }

  protected static Function<byte[], BaseRandom> wrapLongCreatorAsByteArrayCreator(
      final LongFunction<Random> legacyCreator) {
    return new Function<byte[], BaseRandom>() {
      @Override public BaseRandom apply(byte[] bytes) {
        return new RandomWrapper(legacyCreator.apply(convertBytesToLong(bytes)));
      }
    };
  }

  /**
   * Not supported, because this class uses a thread-local seed.
   * @param seedGenerator ignored.
   * @throws UnsupportedOperationException always.
   */
  @Override public void setSeedGenerator(final SeedGenerator seedGenerator) {
    throw new UnsupportedOperationException("This can't be reseeded by a RandomSeederThread");
  }

  private void initSubclassTransientFields() {
    threadLocal = new ThreadLocal<BaseRandom>() {
      @Override protected BaseRandom initialValue() {
        return initializer.get();
      }
    };
  }

  @Override protected boolean withProbabilityInternal(final double probability) {
    return getWrapped().withProbabilityInternal(probability);
  }

  @Override public long nextLong(final long bound) {
    return getWrapped().nextLong(bound);
  }

  @Override public int nextInt(final int origin, final int bound) {
    return getWrapped().nextInt(origin, bound);
  }

  @Override public long nextLong(final long origin, final long bound) {
    return getWrapped().nextLong(origin, bound);
  }

  @Override public BaseRandom getWrapped() {
    return threadLocal.get();
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  @Override public void nextBytes(final byte[] bytes) {
    getWrapped().nextBytes(bytes);
  }

  @Override public int nextInt() {
    return getWrapped().nextInt();
  }

  @Override public int nextInt(final int bound) {
    return getWrapped().nextInt(bound);
  }

  @Override protected long nextLongNoEntropyDebit() {
    return getWrapped().nextLongNoEntropyDebit();
  }

  @Override public boolean nextBoolean() {
    return getWrapped().nextBoolean();
  }

  @Override public float nextFloat() {
    return getWrapped().nextFloat();
  }

  @Override public double nextDoubleNoEntropyDebit() {
    return getWrapped().nextDoubleNoEntropyDebit();
  }

  @Override public double nextGaussian() {
    return getWrapped().nextGaussian();
  }

  @Override protected boolean useParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("wrapped on this thread", getWrapped().dump());
  }

  @Override public byte[] getSeed() {
    return getWrapped().getSeed();
  }

  @SuppressWarnings("VariableNotUsedInsideIf") @Override
  protected void setSeedInternal(final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    super.setSeedInternal(DUMMY_SEED);
    if (threadLocal != null) {
      getWrapped().setSeed(seed);
    }
  }

  @Override protected void debitEntropy(final long bits) {
    getWrapped().debitEntropy(bits);
  }

  @Override public long getEntropyBits() {
    return getWrapped().getEntropyBits();
  }

  @SuppressWarnings("VariableNotUsedInsideIf") @Override public int getNewSeedLength() {
    return (threadLocal == null) ? 0
        : ((explicitSeedSize == null) ? getWrapped().getNewSeedLength() : explicitSeedSize);
  }
}
