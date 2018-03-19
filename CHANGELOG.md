# 2.3.1
* Should slightly improve performance of `CellularAutomatonRandom` when seeding and when accessed
  concurrently.

# 2.3.0
* `RandomDotOrgSeedGenerator` can now be configured with a proxy.
* `RandomSeederThread` adds methods for setting thread priority.

# 2.2.0
* Logging is now done through `slf4j`.
* Removes the class `LooperThread.DummyTarget`, which was only used for deserialization.
* Fixes a bug in `Pcg64Random` that was biasing `nextBoolean` slightly toward false.
* Performance refactors.

# 2.1.0
* Fixes many bugs that were affecting reproducibility when concurrent access occurred, especially
  for longs and doubles.
* Logging in `RandomSeederThread` can now be disabled.
* `LooperThread` is no longer `Serializable`.

# 2.0.0
* Fixes bugs affecting `RandomSeederThread` when a `SeedGenerator` throws a `SeedException`.
* Removes the non-inherited instance methods of `RandomSeederThread` and replaces them with static
  ones, to fix race conditions that can occur when a `RandomSeederThread` shuts down.
* Rename `recordEntropySpent` to `debitEntropy`
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
