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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.corant.shared.normal.Defaults;
import org.corant.shared.ubiquity.Experimental;

/**
 * corant-shared
 *
 * <p>
 * Note: No thread-safe
 *
 * @author bingo 下午12:06:39
 *
 */
@Experimental
public class RandomAccessFileLineInputStream extends RandomAccessFileInputStream {

  private static final int bufferThreshold = (int) (8 * Defaults.ONE_MB);
  private final int bufferIncrement;
  private final CharsetDecoder decoder;
  private byte[] lineBuffer = null;

  public RandomAccessFileLineInputStream(File file) {
    this(file, StandardCharsets.UTF_8);
  }

  public RandomAccessFileLineInputStream(File file, Charset charset) {
    this(file, charset, Defaults.SIXTEEN_KBS);
  }

  public RandomAccessFileLineInputStream(File file, Charset charset, int bufferIncrement) {
    super(file);
    decoder = defaultObject(charset, StandardCharsets.UTF_8).newDecoder();
    decoder.onMalformedInput(CodingErrorAction.REPORT);
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    this.bufferIncrement = max(bufferIncrement, Defaults.FOUR_KBS);
  }

  public RandomAccessFileLineInputStream(RandomAccessFile file) {
    this(file, StandardCharsets.UTF_8);
  }

  public RandomAccessFileLineInputStream(RandomAccessFile file, Charset charset) {
    this(file, charset, 0);
  }

  public RandomAccessFileLineInputStream(RandomAccessFile file, Charset charset,
      int bufferIncrement) {
    super(file);
    decoder = charset.newDecoder();
    decoder.onMalformedInput(CodingErrorAction.REPORT);
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    this.bufferIncrement = max(bufferIncrement, Defaults.FOUR_KBS);
  }

  public final synchronized String readLine() throws IOException {
    byte[] buffer = lineBuffer;
    if (buffer == null || buffer.length > bufferThreshold) {
      buffer = lineBuffer = new byte[256];
    }
    int c = -1;
    int limit = lineBuffer.length;
    int offset = 0;
    boolean eol = false;
    while (!eol) {
      switch (c = read()) {
        case -1:
        case '\n':
          eol = true;
          break;
        case '\r':
          eol = true;
          long cur = file.getFilePointer();
          if ((read()) != '\n') {
            seek(cur);
          }
          break;
        default:
          if (--limit < 0) { // reach limit, need to grow.
            if (buffer.length < bufferIncrement) {
              buffer = new byte[buffer.length * 2];
            } else {
              buffer = new byte[buffer.length + bufferIncrement];
            }
            limit = buffer.length - offset - 1;
            System.arraycopy(lineBuffer, 0, buffer, 0, offset);
            lineBuffer = buffer;
          }
          buffer[offset++] = (byte) c;
          break;
      }
    }
    if ((c == -1) && (offset == 0)) {
      return null;
    }
    return decoder.decode(ByteBuffer.wrap(buffer, 0, offset)).toString();
  }
}
