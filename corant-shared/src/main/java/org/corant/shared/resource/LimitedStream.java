/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.resource;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 上午10:02:03
 *
 */
public interface LimitedStream {

  long getRemaining();

  /**
   * corant-shared
   *
   * <p>
   * Wraps a {@link InputStream}, limiting the number of bytes which can be read.
   *
   * @author bingo 下午2:43:21
   *
   */
  class LimitedInputStream extends FilterInputStream implements LimitedStream {

    private long limit;
    private long mark = -1L;

    public LimitedInputStream(InputStream delegate, long limit) {
      super(shouldNotNull(delegate));
      shouldBeTrue(limit >= 0L, "Parameter error, the given limit must be non-negative");
      this.limit = limit;
    }

    @Override
    public int available() throws IOException {
      return (int) Math.min(in.available(), limit);
    }

    @Override
    public void close() throws IOException {
      super.close();
      limit = 0L;
      mark = -1L;
    }

    @Override
    public long getRemaining() {
      return limit;
    }

    @Override
    public synchronized void mark(int readLimit) {
      if (markSupported()) {
        in.mark(readLimit);
        mark = limit;
      }
    }

    @Override
    public int read() throws IOException {
      final long current = limit;
      if (current <= 0L) {
        return -1;
      }

      int result = in.read();
      if (result != -1) {
        limit = current - 1;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      final long current = limit;
      if (current == 0L) {
        return -1;
      }

      len = (int) Math.min(len, current);
      int result = in.read(b, off, len);
      if (result != -1) {
        limit = current - result;
      }
      return result;
    }

    @Override
    public synchronized void reset() throws IOException {
      final long currMark = mark;
      if (!in.markSupported()) {
        throw new IOException("Mark not supported");
      }
      if (currMark == -1L) {
        throw new IOException("Mark not set");
      }

      in.reset();
      limit = currMark;
    }

    @Override
    public long skip(long n) throws IOException {
      final long current = limit;
      n = Math.min(n, current);
      long skipped = in.skip(n);
      if (skipped > 0L) {
        limit = current - skipped;
      }
      return skipped;
    }
  }

  /**
   * corant-shared
   *
   * <p>
   * Wraps a {@link OutputStream}, limiting the number of bytes which can be write. Attempting to
   * exceed the fixed limit will result in an exception.
   *
   * @author bingo 下午2:46:08
   *
   */
  class LimitedOutputStream extends FilterOutputStream implements LimitedStream {

    private long limit;

    public LimitedOutputStream(OutputStream delegate, long limit) {
      super(shouldNotNull(delegate));
      this.limit = limit;
    }

    @Override
    public long getRemaining() {
      return limit;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      final long current = limit;
      if (current < len) {
        throw new IOException("Not enough space in output stream");
      }
      try {
        out.write(b, off, len);
        limit = current - len;
      } catch (InterruptedIOException e) {
        limit = current - (e.bytesTransferred & 0xFFFFFFFFL);
        throw e;
      }
    }

    @Override
    public void write(int b) throws IOException {
      final long current = limit;
      if (current < 1) {
        throw new IOException("Not enough space in output stream");
      }
      out.write(b);
      limit = current - 1;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午4:39:02
   *
   */
  class RangedInputStream extends LimitedInputStream {

    public RangedInputStream(InputStream delegate, long offset, long limit) {
      super(delegate, limit);
      try {
        delegate.skip(Math.max(0L, offset));
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}
