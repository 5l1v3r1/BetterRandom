// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.util;

import static java.lang.ThreadLocal.withInitial;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nullable;

/**
 * Utility methods for working with binary and hex data.
 * @author Daniel Dyer
 */
public enum BinaryUtils {
  ;

  private static final ThreadLocal<byte[]> LONG_BYTE_ARRAY =
      withInitial(() -> new byte[Long.BYTES]);
  private static final ThreadLocal<ByteBuffer> LONG_BYTE_BUFFER =
      withInitial(() -> ByteBuffer.wrap(LONG_BYTE_ARRAY.get()).order(ByteOrder.nativeOrder()));
  private static final ThreadLocal<byte[]> INT_BYTE_ARRAY =
      withInitial(() -> new byte[Integer.BYTES]);
  private static final ThreadLocal<ByteBuffer> INT_BYTE_BUFFER =
      withInitial(() -> ByteBuffer.wrap(INT_BYTE_ARRAY.get()).order(ByteOrder.nativeOrder()));

  // Mask for casting a byte to an int, bit-by-bit (with
  // bitwise AND) with no special consideration for the sign bit.
  private static final int BITWISE_BYTE_TO_INT = 0x000000FF;

  private static final char[] HEX_CHARS =
      {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  /**
   * Converts an array of bytes into a String of hexadecimal characters (0 - F).
   * @param data An array of bytes to convert to a String.
   * @return A hexadecimal String representation of the data.
   */
  public static String convertBytesToHexString(@Nullable final byte[] data) {
    if (data == null) {
      return "null";
    }
    final StringBuilder buffer = new StringBuilder(data.length * 2);
    for (final byte b : data) {
      buffer.append(HEX_CHARS[(b >>> 4) & 0x0F]);
      buffer.append(HEX_CHARS[b & 0x0F]);
    }
    return buffer.toString();
  }
  
  /**
   * Converts an array of ints into a String of hexadecimal characters (0 - F), taking only the
   * least significant byte of each int. Prepends a newline, unless returning "null".
   * @param data An array of ints to convert to a String.
   * @return A hexadecimal String representation of the data.
   */
  public static String convertIntLeastBytesToHexString(@Nullable final int[] data) {
    if (data == null) {
      return "null";
    }
    final StringBuilder buffer = new StringBuilder(data.length * 2 + 1);
    buffer.append('\n');
    for (final int b : data) {
      buffer.append(HEX_CHARS[(b >>> 4) & 0x0F]);
      buffer.append(HEX_CHARS[b & 0x0F]);
    }
    return buffer.toString();
  }

  /**
   * Converts a hexadecimal String (such as one generated by the {@link
   * #convertBytesToHexString(byte[])} method) into an array of bytes.
   * @param hex The hexadecimal String to be converted into an array of bytes.
   * @return An array of bytes that.
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static byte[] convertHexStringToBytes(
      final String hex) {
    if ((hex.length() % 2) != 0) {
      throw new IllegalArgumentException("Hex string must have even number of characters.");
    }
    final byte[] seed = new byte[hex.length() / 2];
    for (int i = 0; i < seed.length; i++) {
      final int index = i * 2;
      seed[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
      // Can't use Byte.parseByte since it expects signed
    }
    return seed;
  }

  /**
   * Take four bytes from the specified position in the specified block and convert them into a
   * 32-bit int, using the big-endian convention.
   * @param bytes The data to read from.
   * @param offset The position to start reading the 4-byte int from.
   * @return The 32-bit integer represented by the four bytes.
   */
  public static int convertBytesToInt(final byte[] bytes, final int offset) {
    return (BITWISE_BYTE_TO_INT & bytes[offset + 3]) | ((BITWISE_BYTE_TO_INT & bytes[offset + 2])
        << 8) | ((BITWISE_BYTE_TO_INT & bytes[offset + 1]) << 16) | (
        (BITWISE_BYTE_TO_INT & bytes[offset]) << 24);
  }

  /**
   * Convert an array of bytes into an array of ints.  4 bytes from the input data map to a single
   * int in the output data.
   * @param bytes The data to read from.
   * @return An array of 32-bit integers constructed from the data.
   * @since 1.1
   */
  public static int[] convertBytesToInts(final byte[] bytes) {
    if ((bytes.length % 4) != 0) {
      throw new IllegalArgumentException("Number of input bytes must be a multiple of 4.");
    }
    final int[] ints = new int[bytes.length / 4];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = convertBytesToInt(bytes, i * 4);
    }
    return ints;
  }

  /**
   * Utility method to convert an array of bytes into a long.  Byte ordered is assumed to be
   * big-endian.
   * @param bytes The data to read from.
   * @param offset The position to start reading the 8-byte long from.
   * @return The 64-bit integer represented by the eight bytes.
   */
  public static long convertBytesToLong(final byte[] bytes, final int offset) {
    System.arraycopy(bytes, offset, LONG_BYTE_ARRAY, 0, Long.BYTES);
    return LONG_BYTE_BUFFER.get().getLong(0);
  }

  /**
   * Converts a long to an array of bytes.
   * @param input a long.
   * @return an array of 8 bytes containing the long's value in
   *     {@link java.nio.ByteOrder#BIG_ENDIAN} order.
   */
  public static byte[] convertLongToBytes(final long input) {
    LONG_BYTE_BUFFER.get().putLong(0, input);
    return LONG_BYTE_ARRAY.get();
  }

  /**
   * <p>convertIntToBytes.</p>
   * @param input an int.
   * @return an array of 4 bytes containing the int's value in
   *     {@link java.nio.ByteOrder#BIG_ENDIAN} order.
   */
  public static byte[] convertIntToBytes(final int input) {
    INT_BYTE_BUFFER.get().putInt(0, input);
    return INT_BYTE_ARRAY.get();
  }

  /**
   * Convert a byte array to a long, reversing {@link #convertLongToBytes(long)}.
   * @param bytes a byte array of length {@link Long#BYTES} in
   *     {@link java.nio.ByteOrder#BIG_ENDIAN} order.
   * @return {@code bytes} as a long.
   */
  public static long convertBytesToLong(final byte[] bytes) {
    return convertBytesToLong(bytes, 0);
  }
}
