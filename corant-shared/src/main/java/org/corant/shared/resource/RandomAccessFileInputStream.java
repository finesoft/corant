/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * corant-shared
 * <p>
 * <b> NOTE: ALL CODE IN THIS CLASS COPY FROM APACHE COMMON IO, IF THERE IS INFRINGEMENT, PLEASE
 * INFORM ME(finesoft@gmail.com). </b>
 *
 * @author bingo 上午12:14:53
 *
 */
public class RandomAccessFileInputStream extends InputStream {

  protected final RandomAccessFile file;

  public RandomAccessFileInputStream(RandomAccessFile file) {
    this.file = shouldNotNull(file);
  }

  public long availableLong() throws IOException {
    return file.length() - file.getFilePointer();
  }

  @Override
  public void close() throws IOException {
    super.close();
    file.close();
  }

  public RandomAccessFile getFile() {
    return file;
  }

  @Override
  public int read() throws IOException {
    return file.read();
  }

  @Override
  public int read(final byte[] bytes) throws IOException {
    return file.read(bytes);
  }

  @Override
  public int read(final byte[] bytes, final int offset, final int length) throws IOException {
    return file.read(bytes, offset, length);
  }

  public void seek(final long position) throws IOException {
    file.seek(position);
  }

  @Override
  public long skip(final long skipCount) throws IOException {
    if (skipCount <= 0) {
      return 0;
    }
    long filePointer = file.getFilePointer();
    long fileLength = file.length();
    if (filePointer >= fileLength) {
      return 0;
    }
    long targetPos = filePointer + skipCount;
    long newPos = targetPos > fileLength ? fileLength - 1 : targetPos;
    if (newPos > 0) {
      seek(newPos);
    }
    return file.getFilePointer() - filePointer;
  }
}
