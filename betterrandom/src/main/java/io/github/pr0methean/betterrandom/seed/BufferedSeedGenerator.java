package io.github.pr0methean.betterrandom.seed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A seed generator that wraps another, maintaining a buffer of previously-downloaded bytes to
 * reduce the number of I/O calls.
 */
public class BufferedSeedGenerator implements SeedGenerator {
  private static final long serialVersionUID = -2100305696539110970L;

  private final SeedGenerator delegate;
  private final Lock lock = new ReentrantLock();
  private final int size;
  private transient byte[] buffer;
  private transient volatile int pos;

  public BufferedSeedGenerator(SeedGenerator delegate, int size) {
    this.delegate = delegate;
    this.size = size;
    initTransientFields();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  private void initTransientFields() {
    buffer = new byte[size];
    pos = size;
  }

  @Override public void generateSeed(byte[] output) throws SeedException {
    if (output.length >= size) {
      delegate.generateSeed(output);
    } else {
      lock.lock();
      try {
        int available = size - pos;
        if (available >= output.length) {
          System.arraycopy(buffer, pos, output, 0, output.length);
          pos += output.length;
        } else {
          System.arraycopy(buffer, pos, output, 0, available);
          delegate.generateSeed(buffer);
          pos = output.length - available;
          System.arraycopy(buffer, 0, output, available, pos);
        }
      } finally {
        lock.unlock();
      }
    }
  }

  @Override public boolean isWorthTrying() {
    return pos < size || delegate.isWorthTrying();
  }

  @Override public String toString() {
    return String.format("BufferedSeedGenerator(%s,%d)", delegate, size);
  }
}
