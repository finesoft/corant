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
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Streams.streamOf;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 上午10:21:10
 *
 */
public class Texts {

  private Texts() {}

  /**
   * Convert input stream to string
   *
   * @param is
   * @return
   * @throws IOException fromInputStream
   */
  public static String fromInputStream(InputStream is) throws IOException {
    return fromInputStream(is, StandardCharsets.UTF_8);
  }

  /**
   * Convert input stream to string
   *
   * @param is
   * @param charset
   * @return
   * @throws IOException fromInputStream
   */
  public static String fromInputStream(InputStream is, Charset charset) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(is, charset))) {
      int c = 0;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
      }
    }
    return sb.toString();
  }

  public static boolean isZhChar(char c) {
    Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
    return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
        || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
        || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
        || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
  }

  public static boolean isZhChar(int c) {
    return isZhChar((char) c);
  }

  /**
   * String lines from file, use for read txt file line by line.
   *
   * @param file
   * @return lines
   */
  public static Stream<String> lines(final File file) {
    FileInputStream fis;
    try {
      fis = new FileInputStream(shouldNotNull(file));
    } catch (FileNotFoundException e1) {
      throw new CorantRuntimeException(e1);
    }
    return lines(fis, -1, -1).onClose(() -> {
      try {
        fis.close();
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  /**
   * String lines from file input stream, use for read txt file line by line.
   *
   * @param fis
   * @param offset the offset start from 0
   * @param limit the number of lines returned
   */
  public static Stream<String> lines(final FileInputStream fis, int offset, int limit) {
    return lines(new InputStreamReader(fis, StandardCharsets.UTF_8), offset,
        (i, t) -> limit >= 1 && i > limit);
  }

  /**
   * String lines from input stream, use for read txt file line by line.
   *
   * @param is
   */
  public static Stream<String> lines(final InputStream is) {
    return lines(new InputStreamReader(is, StandardCharsets.UTF_8), 0, null);
  }

  /**
   * String lines from input stream, use for read txt line by line.
   *
   * @param is
   * @param charset the charset
   * @param offset use to skip lines, the offset start from 0
   * @param terminator use to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStream is, Charset charset, int offset,
      BiPredicate<Integer, String> terminator) {
    return lines(new InputStreamReader(is, charset), offset, terminator);
  }

  /**
   * String lines from input stream, use for read txt file line by line.
   *
   * @param is
   * @param offset use to skip lines, the offset start from 0
   * @param terminator use to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStream is, int offset,
      BiPredicate<Integer, String> terminator) {
    return lines(new InputStreamReader(is, StandardCharsets.UTF_8), offset, terminator);
  }

  /**
   * String lines from input stream reader, use for read txt file line by line.
   *
   * @param isr the input stream reader
   * @param offset the offset start from 0
   * @param terminator use to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStreamReader isr, int offset,
      BiPredicate<Integer, String> terminator) {

    return streamOf(new Iterator<String>() {

      final BufferedReader reader = new BufferedReader(isr);
      final BiPredicate<Integer, String> useTerminator =
          terminator == null ? (i, t) -> false : terminator;
      String nextLine = null;
      int readLines = 0;
      boolean valid = true;
      // skip lines if necessary
      {
        try {
          if (offset > 0) {
            for (int i = 0; i < offset; i++) {
              if (reader.readLine() == null) {
                valid = false;
                break;
              }
            }
          }
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        }
      }

      @Override
      public boolean hasNext() {
        if (!valid) {
          return false;
        }
        if (nextLine != null) {
          return true;
        } else {
          try {
            nextLine = reader.readLine();
            return nextLine != null && !useTerminator.test(++readLines, nextLine);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
      }

      @Override
      public String next() {
        if (nextLine != null || hasNext()) {
          String line = nextLine;
          nextLine = null;
          return line;
        } else {
          throw new NoSuchElementException();
        }
      }
    });
  }

  public static Stream<String> lines(final String filePath) {
    return lines(new File(filePath));
  }

  public static List<String> readFromFile(String path) {
    return Texts.lines(new File(path)).collect(Collectors.toList());
  }

  /**
   * Convert input stream to String
   *
   * @param is
   * @return tryFromInputStream
   */
  public static String tryFromInputStream(InputStream is) {
    try {
      return fromInputStream(is);
    } catch (IOException e) {
      return null;
    }
  }

  public static void writeToFile(File file, boolean append, Charset charset, Stream<String> lines)
      throws IOException {
    if (!file.exists()) {
      shouldBeTrue(file.createNewFile());
    }
    try (OutputStream os = new FileOutputStream(file, append);
        BufferedWriter fileWritter = new BufferedWriter(new OutputStreamWriter(os, charset))) {
      lines.forEach(line -> {
        try {
          fileWritter.append(line);
          fileWritter.newLine();
          fileWritter.flush();
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        }
      });
    }
  }

  public static void writeToFile(File file, boolean append, Stream<String> lines)
      throws IOException {
    writeToFile(file, append, StandardCharsets.UTF_8, lines);
  }

  public static void writeToFile(File file, Iterable<String> data) throws IOException {
    writeToFile(file, true, streamOf(data));
  }
}
