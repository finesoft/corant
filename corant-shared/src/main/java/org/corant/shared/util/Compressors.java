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
import static org.corant.shared.util.Streams.copy;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
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
import java.nio.file.Path;
import java.util.LinkedList;
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

  public static void unzip(Path from, Path to) throws IOException {
    File src = from.toFile();
    File dest = to.toFile();
    shouldBeTrue(src.exists());
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(src))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        File newFile = new File(dest, zipEntry.getName());
        shouldBeTrue(
            newFile.getCanonicalPath().startsWith(dest.getCanonicalPath() + File.separator));
        if (zipEntry.getName().endsWith(File.separator)) {
          if (!newFile.isDirectory() && !newFile.mkdirs()) {
            throw new IOException("Unzip error, failed to create directory " + newFile);
          }
        } else {
          File parent = newFile.getParentFile(); // fix for Windows-created archives
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unzip error, failed to create directory " + parent);
          }
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            copy(zis, fos);
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  public static void zip(Path from, Path to) throws IOException {
    File fromFile = from.toFile();
    File toFile = to.toFile();
    shouldBeTrue(fromFile.exists());
    try (FileOutputStream os = new FileOutputStream(toFile);
        CheckedOutputStream cos = new CheckedOutputStream(os, new CRC32());
        ZipOutputStream zos = new ZipOutputStream(cos)) {
      LinkedList<Pair<File, Path>> fileAndDirs = linkedListOf(Pair.of(fromFile, Path.of(EMPTY)));
      Pair<File, Path> fileAndDir = null;
      while ((fileAndDir = fileAndDirs.poll()) != null) {
        File file = fileAndDir.left();
        Path dir = fileAndDir.right();
        if (file.isDirectory()) {
          File[] subFiles = file.listFiles();
          if (isNotEmpty(subFiles)) {
            streamOf(subFiles).map(sf -> Pair.of(sf, dir.resolve(file.getName())))
                .forEach(fileAndDirs::offer);
          } else {
            zos.putNextEntry(new ZipEntry(dir.resolve(file.getName()).toString() + File.separator));
          }
          zos.closeEntry();
        } else {
          try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            zos.putNextEntry(new ZipEntry(dir.resolve(file.getName()).toString()));
            copy(bis, zos);
            zos.closeEntry();
          }
        }
      }
    }
  }
}
