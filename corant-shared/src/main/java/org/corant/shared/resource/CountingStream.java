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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * corant-shared
 *
 * @author bingo 上午10:00:10
 */
public interface CountingStream {

  long getCount();

  /**
   * corant-shared
   *
   * @author bingo 上午10:00:51
   *
   */
  class CountingInputStream extends FilterInputStream implements CountingStream {

    private long count;
    private long mark = -1;

    public CountingInputStream(InputStream in) {
      super(shouldNotNull(in));
    }

    @Override
    public long getCount() {
      return count;
    }

    @Override
    public synchronized void mark(int readlimit) {
      in.mark(readlimit);
      mark = count;
    }

    @Override
    public int read() throws IOException {
      int result = in.read();
      if (result != -1) {
        count++;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int result = in.read(b, off, len);
      if (result != -1) {
        count += result;
      }
      return result;
    }

    @Override
    public synchronized void reset() throws IOException {
      if (!in.markSupported()) {
        throw new IOException("Mark not supported");
      }
      if (mark == -1) {
        throw new IOException("Mark not set");
      }

      in.reset();
      count = mark;
    }

    @Override
    public long skip(long n) throws IOException {
      long result = in.skip(n);
      count += result;
      return result;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:01:18
   *
   */
  class CountingOutputStream extends FilterOutputStream implements CountingStream {

    private long count;

    public CountingOutputStream(OutputStream out) {
      super(shouldNotNull(out));
    }

    @Override
    public void close() throws IOException {
      out.close();
    }

    @Override
    public long getCount() {
      return count;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
      count += len;
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
      count++;
    }
  }
}
