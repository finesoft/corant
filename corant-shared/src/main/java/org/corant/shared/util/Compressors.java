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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.linkedListOf;
import static org.corant.shared.util.Objects.isNoneNull;
import static org.corant.shared.util.Streams.copy;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.isBlank;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

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
        DeflaterOutputStream dos = new DeflaterOutputStream(os, def, 4096, false)) {
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

  public static void unzip(File zipFile, Charset charset, File destFile) throws IOException {
    shouldBeTrue(isNoneNull(zipFile, charset, destFile) && zipFile.exists());
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), charset)) {
      ZipEntry zipEntry = zis.getNextEntry();
      final String destDir = destFile.getCanonicalPath() + File.separator;
      while (zipEntry != null) {
        File file = new File(destFile, zipEntry.getName());
        shouldBeTrue(file.getCanonicalPath().startsWith(destDir));
        if (zipEntry.isDirectory()) {
          if (!file.isDirectory() && !file.mkdirs()) {
            throw new IOException("Unzip error, failed to create directory " + file);
          }
        } else {
          File parent = file.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unzip error, failed to create directory " + parent);
          }
          try (FileOutputStream fos = new FileOutputStream(file)) {
            copy(zis, fos);
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  public static void unzip(File zipFile, File destDir) throws IOException {
    unzip(zipFile, StandardCharsets.UTF_8, destDir);
  }

  public static void zip(File file, File zipFile) throws IOException {
    shouldBeTrue(file != null && file.exists());
    try (FileOutputStream os = new FileOutputStream(zipFile);
        CheckedOutputStream cos = new CheckedOutputStream(os, new CRC32());
        ZipOutputStream zos = new ZipOutputStream(cos)) {
      zip(file, EMPTY, zos);
    }
  }

  public static void zip(List<File> files, File zipFile) throws IOException {
    if (isNotEmpty(files)) {
      try (FileOutputStream os = new FileOutputStream(zipFile);
          CheckedOutputStream cos = new CheckedOutputStream(os, new CRC32());
          ZipOutputStream zos = new ZipOutputStream(cos)) {
        for (File file : files) {
          if (file.exists()) {
            zip(file, EMPTY, zos);
          }
        }
      }
    }
  }

  static void zip(File file, String dir, ZipOutputStream zos) throws IOException {
    shouldBeTrue(file.exists());
    LinkedList<Pair<File, String>> sources = linkedListOf(Pair.of(file, dir));
    Pair<File, String> source = null;
    while ((source = sources.poll()) != null) {
      File curFile = source.left();
      String parent = source.right();
      String path = isBlank(parent) ? curFile.getName() : parent + "/" + curFile.getName();
      if (curFile.isDirectory()) {
        File[] subFiles = curFile.listFiles();
        if (isNotEmpty(subFiles)) {
          streamOf(subFiles).map(f -> Pair.of(f, path)).forEach(sources::offer);
        } else {
          zos.putNextEntry(new ZipEntry(path + "/"));
        }
        zos.closeEntry();
      } else {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(curFile))) {
          zos.putNextEntry(new ZipEntry(path));
          copy(bis, zos);
          zos.closeEntry();
        }
      }
    }
  }

}
