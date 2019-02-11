# 3.1.0
* `RandomSeederThread` now exposes the constant `DEFAULT_DEFAULT_PRIORITY`.

# 3.0.3
* `RandomSeederThread`'s default priority is increased slightly, to decrease the risk that it is
  starved out. It is still "best efforts", but best efforts are now more likely to suffice.

# 3.0.2
* Subpackage `adapter` has been renamed `concurrent`.
* `CipherCounterRandom.doCipher` overrides can now throw any `GeneralSecurityException`, not just
  the subclasses that the method throws in `AesCounterRandom`.
* Fixes Javadoc warnings.
* Simplifies pom.xml for Javadoc generation.

# 3.0.1
* Update dependencies.

# 3.0.0
* `CipherCounterRandom` is now compatible with non-JCE ciphers, with a proof-of-concept
  implementation in the test root using the Bouncy Castle implementation of Twofish.

# 2.9.0
* `AesCounterRandom` now has a cipher-agnostic superclass, `CipherCounterRandom`.

# 2.8.1
* `readObjectNoData` no longer pretends to succeed. Deserializing a subclass of `BaseRandom` will
  now fail with an `InvalidObjectException` if the serialized version did not extend `BaseRandom`.
* `testReseeding` for some adapter PRNG tests should now be much less flaky.
* Update documentation for `RandomDotOrgSeedGenerator`.

# 2.8.0
* `DefaultSeedGenerator` can now be configured to delegate to any `SeedGenerator` instance.
* Adds `BufferedSeedGenerator`, a delegating implementation of `SeedGenerator` with a buffer to
  reduce the number of I/O calls.
* The default `DefaultSeedGenerator` now uses a 128-byte buffer when it calls
  `DevRandomSeedGenerator`.
* Adds `SeedGeneratorPreferenceList`, an implementation of `SeedGenerator` with a list of delegates
  that tries them until one succeeds.
* Tests of `RandomDotOrgSeedGenerator` for large requests are now hermetic.

# 2.7.1
* Refactored `ThreadLocalRandomWrapper.getSeedGenerator()`.

# 2.7.0
* Moves `*RandomWrapper` to the subpackage `io.github.pr0methean.betterrandom.prng.adapter`.
* `BaseRandom.useParallelStreams` is now named `usesParallelStreams` and is now a public method,
  since it affects wrapper behavior.

# 2.6.0
* Adds `Pcg128Random`, a variant of `Pcg64Random` that uses a 128-bit seed and state.

# 2.5.1
* `AesCounterRandom` now reuses the `MessageDigest` instance when reseeding. This should improve
  performance.

# 2.5.0
* `RandomSeederThread.asyncReseed` has been replaced with `wakeUp`, and the task queue has been
  eliminated (since if the thread is locked, it is already awake or being awoken).
* `ReseedingSplittableRandomAdapter` should now reseed itself more reliably.

# 2.4.8
* `DevRandomSeedGenerator` and `RandomDotOrgSeedGenerator` now use buffered input streams to reduce
  the number of I/O calls.
* Now uses `Double.toRawLongBits` instead of `Double.toLongBits` when generating doubles with an
  explicit range; this should be slightly faster.

# 2.4.7
* No longer uses `ByteBuffer` for type conversion, because this increased memory usage with no real
  performance benefit. Type conversions have been refactored so that object reuse is as good as
  before, with less reliance on `ThreadLocal<byte[]>` objects.

# 2.4.6
* All instances of `ByteBuffer` now use the machine's native byte order.


# 2.4.5
* Improves performance of `XorShiftRandom.setSeed()`.
* Fixes a bug that could cause `SplittableRandomAdapter.getSeed()` to return a value other than the
  actual seed for the calling thread.
* Fixes a bug in `RandomWrapper.getSeed()` when wrapping a `RepeatableRandom` that uses thread-local
  seeds; it could previously have returned the seed from a different thread.
* `setSeedGenerator` will now never throw an exception if the seed generator it attempts to set is
  the one already in use, even if changing it isn't supported.

# 2.4.4
* `RandomSeederThread` no longer overrides `Thread.interrupt()`.
* `getSeedGenerator` now behaves as specified when called on a `ReseedingSplittableRandomAdapter` or
  `ReseedingThreadLocalRandomWrapper`.

# 2.4.3
* Reduces unit-test flakiness further on systems where `DefaultSeedGenerator` is slow.

# 2.4.2
* `BaseRandom#getSeedGenerator` and `BaseRandom#setSeedGenerator(SeedGenerator)` no longer give
  misleading results after the `RandomSeederThread` has crashed.
* PRNGs associated with a running `RandomSeederThread` can now be garbage-collected slightly sooner.
* May improve `RandomSeederThread` performance.
* Reduces unit-test flakiness and shortens `RandomSeederThread` lifespan during tests.

# 2.4.1
* `RandomDotOrgSeedGenerator` and `AesCounterRandom` no longer create `Logger` instances eagerly or
  keep references to them after use, since they very rarely log.
* `Cmwc4096RandomTest` now uses a mock `DefaultSeedGenerator`, improving performance on systems
  where the real one is slow.
* Other PRNG tests make less use of `DefaultSeedGenerator`.

# 2.4.0
* Removes `CloneViaSerialization` to the test jar, since it's not used anywhere else.
* Adds `BaseRandom.getSeedGenerator()`.
* Fixes a bug where a PRNG would lose its SeedGenerator upon deserialization.
* Slightly improves unit-test parallelization.
* Fixes a rare flake in `testSetSeedZero`.

# 2.3.14
* `RandomSeederThread` no longer uselessly uses weak references to its instances. This decreases
  the memory footprint slightly.

# 2.3.12
* Fixes a bug that could cause `*SplittableRandomAdapter` to give correlated results on multiple
  threads.
* Fixes a bug with entropy counting in `ThreadLocalRandomWrapper`.
* Unit tests are now split into 3 groups that run in parallel. One group also has within-group
  parallelism.

# 2.3.11
* `setSeed` now invalidates the result of the next call to `nextGaussian` if it's been pre-computed.
* Fixes a bug where `RandomDotOrgSeedGenerator` could reuse seed data or output all-zeroes seeds.
* Fixes bugs affecting seekability and repeatability in several PRNGs.

# 2.3.10
* If random.org ever changes its API output format in a way that breaks us, or sends a corrupted
  response, then the result will now always be a `SeedException`, and this is now tested through
  fault injection.
* The Java 7 branch is no longer tested on AppVeyor. The versions of Java 7 available as Windows
  binaries do not support TLS 1.2, which Maven Central began to require in June 2018. (Caching had
  masked this issue for some time.)

# 2.3.9
* `RandomSeederThread` fields are now all final, and collection fields are cleared on interrupt.
* `RandomSeederThread`'s error message now indicates which `SeedGenerator` is affected.
* Minor refactoring.

# 2.3.8
* Can now build using the same pom.xml on JDK8 and JDK9.
* Fix a test bug that made `ReseedingThreadLocalRandomWrapperTest.testReseeding()` flaky.
* Use double-checked locking to slightly improve performance of `DevRandomSeedGenerator`.

# 2.3.7
* Reduces the performance cost of calling `BaseRandom.setSeederThread` redundantly.

# 2.3.6
* Coverage and performance improvements to the tests. The performance improvement is large enough to
  justify a new release given that Maven Central includes test jars, despite that no main-jar code
  has changed from 2.3.5.

# 2.3.5
* Fixes a rare race condition while reseeding `CellularAutomatonRandom`.

# 2.3.4
* Should improve performance of `CellularAutomatonRandom` when reseeding frequently, especially
  under heavy GC load.

# 2.3.3
* Fixes a bug where `RandomDotOrgSeedGenerator` didn't use a proxy when configured with one.
* `RandomDotOrgSeedGenerator` now closes the connection when done with it.
* `RandomDotOrgSeedGenerator` will now gracefully handle receiving more bytes than requested.

# 2.3.2
* Removes unnecessary `synchronized` modifier from some methods.
* Tests now include an additional sanity check for `SetSeed`: a seed of all zeroes should give
  different output than a normal seed.

# 2.3.1
* Should slightly improve performance of `CellularAutomatonRandom` when seeding and when accessed
  concurrently.

# 2.3.0
* `RandomDotOrgSeedGenerator` can now be configured with a proxy.
* `RandomSeederThread` adds methods for setting thread priority.

# 2.2.0
* Logging is now done through `slf4j`.
* Removes the class `LooperThread.DummyTarget`, which was only used for deserialization.
* Performance refactors.
* Fixes a bug in `Pcg64Random` that was biasing `nextBoolean` slightly toward false.

# 2.1.0
* Fixes many bugs that were affecting reproducibility when concurrent access occurred, especially
  for longs and doubles.
* Logging in `RandomSeederThread` can now be disabled.
* `LooperThread` is no longer `Serializable`.

# 2.0.0
* Fixes bugs affecting `RandomSeederThread` when a `SeedGenerator` throws a `SeedException`.
* Removes the non-inherited instance methods of `RandomSeederThread` and replaces them with static
  ones, to fix race conditions that can occur when a `RandomSeederThread` shuts down.
* Refactorings to slightly speed up construction and deserialization and slightly shrink the jar.

# 1.3.2
* Bug fixes for `LooperThread`, mainly affecting `awaitIteration`.
* Tests should no longer be flaky on OSX.

# 1.3.1
* Fix a javadoc error.

# 1.3.0
* New algorithm: `Pcg64Random`.
* Bug fix: `AesCounterRandom` was not crediting enough entropy after being reseeded.

# 1.2.1
* Bug fix: `RandomWrapper.wrapped` needs to be volatile, despite its lock guard, because it may be
  accessed from multiple threads.

# 1.2.0
* Added support for random.org's new JSON API.

# 1.1.2
* Improved performance of DevRandomSeedGenerator by reusing the FileInputStream.

# 1.1.1
* SeedException is now a RuntimeException rather than a checked exception.

# 1.1.0
* Random-number streams are no longer falsely advertised as parallel.
* Added `ThreadLocalRandomWrapper`, which is a thread-local PRNG backed by any
  `Supplier<BaseRandom>`. It does create parallel streams.
* Fixed a crash when reseeding a `RandomWrapper` whose underlying `Random` is a
  `ByteArrayReseedableRandom` that doesn't accept 8-byte seeds.
* Eliminated `io.github.pr0methean.betterrandom.util.spliterator`.
* Reduced the memory and class-loading footprint of random-number streams.

# 1.0.2
* Fixed a crash on Java 9 when reseeding an AesCounterRandom.

# 1.0.1
* Fixed a bug involving uncaught-exception handlers for a `LooperThread`.
* Added several useful new constructor overloads.

# 1.0.0
* Fixed entropy counting. After obtaining a stream from one of the `ints`, `longs` or `doubles`
  methods, you can now expect it to reseed midstream if `setSeederThread` has been called with
  non-null argument.
* Removed `SplittableRandomReseeder` since it is unreliable on some platforms (probably due to
  `SplittableRandom`'s non-volatile access to its fields).
* Removed default implementation of `BaseSplittableRandomAdapter.addSubclassFields` since it's
  neither used nor tested within this library.
* Added `BaseRandom.nextElement` and `BaseRandom.nextEnum` convenience methods.

# 0.10.2
* Improved performance of `DefaultSeedGenerator` on MinGW and naked Windows, especially when
  random.org is unavailable and the number of CPU cores is limited.

# 0.10.1
* Fixed some bugs involving serialization and deserialization of `LooperThread`.
* Scripts that are not actively maintained now live in the `cutting_room_floor` folder.

# 0.10.0
* Release jars are now optimized by Proguard.
* Fixed a bug where `BaseRandom` would crash if `setSeed(long)` was called more than once.
* Merged `BaseEntropyCountingRandom` into `BaseRandom`.
* Improvements to test coverage.

# 0.9.2
* Refactorings to increase object reuse, and therefore throughput, in all PRNGs, especially
  `ReseedingSplittableRandomAdapter` (which improved benchmark throughput by a factor of over 300).
* Benchmarks are now available on GitHub.
* `JavaRandom` is now `RandomWrapper`, and can take any Random instance as the one to wrap.
* `RandomWrapper` and `SplittableRandomAdapter` now count entropy.

# 0.9.1.1
* First release on Maven Central.
