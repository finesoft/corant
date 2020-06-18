/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.util;

import static org.corant.shared.util.Streams.copy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 * <p>
 * Simple compress tool class, use to compress / decompress bytes.
 * </p>
 *
 * @author bingo 下午12:59:23
 *
 */
public class Compressors {

  private Compressors() {}

  public static byte[] compress(byte[] bytes) throws IOException {
    try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      compress(is, os);
      return os.toByteArray();
    }
  }

  public static void compress(InputStream is, OutputStream os) throws IOException {
    final Deflater def = new Deflater(Deflater.DEFAULT_COMPRESSION);
    try (Closeable cdef = (Closeable) def::end;
        DeflaterOutputStream dos = new DeflaterOutputStream(os, def, 4096, false);) {
      copy(is, dos);
    }
  }

  public static void compress(InputStream is, OutputStream os, Deflater def, int size,
      boolean syncFlush) throws IOException {
    try (DeflaterOutputStream dos = new DeflaterOutputStream(os, def, size, syncFlush)) {
      copy(is, dos);
    }
  }

  public static byte[] decompress(byte[] bytes) throws IOException {
    try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      decompress(is, os);
      return os.toByteArray();
    }
  }

  public static void decompress(InputStream is, OutputStream os) throws IOException {
    final Inflater inf = new Inflater();
    try (Closeable cinf = (Closeable) inf::end;
        InflaterInputStream iis = new InflaterInputStream(is, inf, 4096)) {
      copy(iis, os);
    }
  }

  public static void decompress(InputStream is, OutputStream os, Inflater inf, int size)
      throws IOException {
    try (InflaterInputStream iis = new InflaterInputStream(is, inf, size)) {
      copy(iis, os);
    }
  }

  public static byte[] tryCompress(byte[] bytes) {
    try {
      return compress(bytes);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void tryCompress(InputStream is, OutputStream os) {
    try {
      compress(is, os);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static byte[] tryDecompress(byte[] bytes) {
    try {
      return decompress(bytes);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void tryDecompress(InputStream is, OutputStream os) {
    try {
      decompress(is, os);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }
}
