package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertLongToBytes;

/**
 * Collection of arithmetic methods that treat {@code byte[16]} arrays as 128-bit unsigned integers.
 */
@SuppressWarnings("AccessStaticViaInstance")
public enum Byte16ArrayArithmetic {
  ;

  private static final int SIZE_BYTES = 16;
  public static final int SIZE_BYTES_MINUS_LONG = SIZE_BYTES - Long.BYTES;
  public static final byte[] ZERO = new byte[SIZE_BYTES];
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final long UNSIGNED_INT_TO_LONG_MASK = (1L << Integer.SIZE) - 1;

  /**
   * {@code counter += delta}  
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   * @param addendDigits working register
   */
  public static void addInto(final byte[] counter, final long delta, final byte[] addendDigits) {
    BinaryUtils.convertLongToBytes(delta, addendDigits, SIZE_BYTES_MINUS_LONG);
    final byte signExtend = (byte) (delta < 0 ? -1 : 0);
    for (int i = 0; i < SIZE_BYTES_MINUS_LONG; i++) {
      addendDigits[i] = signExtend;
    }
    addInto(counter, addendDigits);
  }

  /**
   * {@code counter += delta}. Inputs must be the same length.
   * @param counter the first input and the result
   * @param delta the second input
   */
  public static void addInto(final byte[] counter, final byte[] delta) {
    boolean carry = false;
    for (int i = 15; i >= 0; i--) {
      final int oldCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      counter[i] += delta[i] + (carry ? 1 : 0);
      final int newCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      carry = (oldCounterUnsigned > newCounterUnsigned)
          || (carry && (oldCounterUnsigned == newCounterUnsigned));
    }
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param mult the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(
      final byte[] counter, final byte[] mult) {
    final long x = convertBytesToLong(counter, Long.BYTES);
    final long y = convertBytesToLong(mult, Long.BYTES);

    // https://stackoverflow.com/a/38880097/833771
    final long x_high = x >>> 32;
    final long x_low = x & UNSIGNED_INT_TO_LONG_MASK;
    final long y_high = y >>> 32;
    final long y_low = y & UNSIGNED_INT_TO_LONG_MASK;
    final long z2 = x_low * y_low;
    final long t = x_high * y_low + (z2 >>> 32);
    long z1 = t & UNSIGNED_INT_TO_LONG_MASK;
    final long z0 = t >>> 32;
    z1 += x_low * y_high;
    final long highOut = x_high * y_high + z0 + (z1 >>> 32) + convertBytesToLong(counter, 0) * y
        + convertBytesToLong(mult, 0) * x;

    final long lowOut = x * y;
    convertLongToBytes(highOut, counter, 0);
    convertLongToBytes(lowOut, counter, Long.BYTES);
  }

  /**
   * {@code counter *= mult; counter += add}
   * @param counter the first input and the result
   * @param mult the input to multiply by
   * @param add the input to add after multiplying
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyIntoAndAddInto(
      final byte[] counter, final byte[] mult, final byte[] add) {
    final long x = convertBytesToLong(counter, Long.BYTES);
    final long y = convertBytesToLong(mult, Long.BYTES);

    // https://stackoverflow.com/a/38880097/833771
    final long x_high = x >>> 32;
    final long x_low = x & UNSIGNED_INT_TO_LONG_MASK;
    final long y_high = y >>> 32;
    final long y_low = y & UNSIGNED_INT_TO_LONG_MASK;
    final long z2 = x_low * y_low;
    final long t = x_high * y_low + (z2 >>> 32);
    long z1 = t & UNSIGNED_INT_TO_LONG_MASK;
    final long z0 = t >>> 32;
    z1 += x_low * y_high;
    final long lowProduct = x * y;
    final long lowOut = lowProduct + convertBytesToLong(add, Long.BYTES);
    final long highOut = (x_high * y_high) + z0 + (z1 >>> 32)
        + (convertBytesToLong(counter, 0) * y)
        + (convertBytesToLong(mult, 0) * x)
        + convertBytesToLong(add, 0)
        + (Long.compareUnsigned(lowProduct, lowOut) > 0 ? 1 : 0);

    convertLongToBytes(highOut, counter, 0);
    convertLongToBytes(lowOut, counter, Long.BYTES);
  }

  private static long trueShiftRight(final long input, final int amount) {
    if (amount <= -Long.SIZE || amount >= Long.SIZE) {
      return 0;
    }
    if (amount < 0) {
      return input << -amount;
    }
    return input >>> amount;
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(final byte[] shifted, final int bits) {
    if (bits == 0) {
      return;
    }
    final long oldMost = convertBytesToLong(shifted);
    final long oldLeast = convertBytesToLong(shifted, Long.BYTES);
    convertLongToBytes(shiftedMost(bits, oldMost, oldLeast), shifted, 0);
    convertLongToBytes(shiftedLeast(bits, oldMost, oldLeast), shifted, Long.BYTES);
  }

  /**
   * {@code return (long)(shifted >>> bits)}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static long unsignedShiftRightLeast64(final byte[] shifted, final int bits) {
    final long oldLeast = convertBytesToLong(shifted, Long.BYTES);
    if (bits == 0) {
      return oldLeast;
    }
    final long oldMost = convertBytesToLong(shifted);
    return shiftedLeast(bits, oldMost, oldLeast);
  }

  /**
   * Returns the upper 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}.
   * @param bits how many bits to shift by
   * @param oldMost upper 64 bits of input
   * @param oldLeast lower 64 bits of input
   * @return the upper 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}
   */
  public static long shiftedMost(final int bits, final long oldMost, final long oldLeast) {
    return trueShiftRight(oldMost, bits) | trueShiftRight(oldLeast, bits + 64);
  }

  /**
   * Returns the lower 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}.
   * @param bits how many bits to shift by
   * @param oldMost upper 64 bits of input
   * @param oldLeast lower 64 bits of input
   * @return the lower 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}
   */
  public static long shiftedLeast(final int bits, final long oldMost, final long oldLeast) {
    return trueShiftRight(oldLeast, bits) | trueShiftRight(oldMost, bits - 64);
  }

  /**
   * {@code shifted = (shifted >>> bits) | shifted << (128 - bits)}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void rotateRight(final byte[] shifted, int bits) {
    bits %= 128;
    if (bits == 0) {
      return;
    }
    if (bits < 0) {
      bits += 128;
    }
    final long oldMost = convertBytesToLong(shifted);
    final long oldLeast = convertBytesToLong(shifted, Long.BYTES);
    convertLongToBytes(
        shiftedMost(bits, oldMost, oldLeast) | shiftedMost(otherShift(bits), oldMost, oldLeast),
        shifted, 0);
    convertLongToBytes(rotateRightLeast64(bits, oldMost, oldLeast),
        shifted, Long.BYTES);
  }

  private static long rotateRightLeast64(final int bits, final long oldMost, final long oldLeast) {
    return shiftedLeast(bits, oldMost, oldLeast) | shiftedLeast(otherShift(bits), oldMost, oldLeast);
  }

  private static int otherShift(final int bits) {
    return bits > 0 ? bits - 128 : bits + 128;
  }

  /**
   * {@code return (long) ((shifted >>> bits) | shifted << (128 - bits))}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static long rotateRightLeast64(final byte[] shifted, final int bits) {
    return rotateRightLeast64(bits, convertBytesToLong(shifted),
        convertBytesToLong(shifted, Long.BYTES));
  }

}
