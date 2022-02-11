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
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.NEWLINE;
import static org.corant.shared.util.Strings.RETURN;
import static org.corant.shared.util.Strings.escapedPattern;
import static org.corant.shared.util.Strings.replace;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
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

  public static final char CSV_FIELD_DELIMITER = ',';
  public static final char CSV_FIELD_QUOTES = '"';
  public static final String CSV_FIELD_DELIMITER_STRING = ",";
  public static final String CSV_FIELD_QUOTES_STRING = "\"";
  public static final String CSV_DOUBLE_QUOTES = "\"\"";
  public static final String XSV_CR_REP = "\\r";
  public static final String XSV_LF_REP = "\\n";
  public static final String XSV_DOUBLE_BACK_SLASH = "\\\\";

  private Texts() {}

  /**
   * CSV rows from file input stream, use for read CSV file line by line.
   *
   * @param file the CSV file
   */
  public static Stream<List<String>> asCSVLines(final File file) {
    return asCSVLines(file, null, -1, -1);
  }

  /**
   * CSV rows from file input stream, use for read CSV file line by line.
   *
   * @param file the CSV file
   * @param charset the CSV file charset
   */
  public static Stream<List<String>> asCSVLines(final File file, final Charset charset) {
    return asCSVLines(file, charset, -1, -1);
  }

  /**
   * CSV rows from file input stream, use for read CSV file line by line.
   *
   * @param file the CSV file
   * @param charset the CSV file charset
   * @param offset the offset start from 0
   * @param limit the number of rows returned streamCSVRows
   */
  public static Stream<List<String>> asCSVLines(final File file, final Charset charset,
      final int offset, final int limit) {
    final FileInputStream fis;
    try {
      fis = new FileInputStream(shouldNotNull(file));
    } catch (FileNotFoundException e1) {
      throw new CorantRuntimeException(e1);
    }
    return asCSVLines(fis, defaultObject(charset, StandardCharsets.UTF_8), offset,
        (i, t) -> limit >= 1 && i > limit).onClose(() -> {
          try {
            fis.close();
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        });
  }

  /**
   * CSV rows from input stream, use for read CSV file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the CSV format input stream
   * @return streamCSVRows
   */
  public static Stream<List<String>> asCSVLines(final InputStream is) {
    return asCSVLines(is, StandardCharsets.UTF_8, 0, null);
  }

  /**
   * CSV rows from input stream, use for read CSV file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the CSV format input stream
   * @param charset
   * @param offset the offset start from 0
   * @param terminator used to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<List<String>> asCSVLines(final InputStream is, final Charset charset,
      final int offset, final BiPredicate<Integer, String> terminator) {
    final BufferedReader reader = new CSVBufferedReader(new InputStreamReader(is, charset));
    return lines(reader, offset, terminator, Texts::readCSVFields);
  }

  /**
   * Convert string to byte array input stream.
   *
   * @param data
   * @return asInputStream
   */
  public static InputStream asInputStream(final String data) {
    return new ByteArrayInputStream(shouldNotNull(data).getBytes());
  }

  /**
   * Convert string to byte array input stream with charset
   *
   * @param data
   * @param charset
   * @return asInputStream
   */
  public static InputStream asInputStream(final String data, final Charset charset) {
    return new ByteArrayInputStream(shouldNotNull(data).getBytes(charset));
  }

  /**
   * Read text file by line and use any character string as a delimiter to split the line into a
   * field list, support delimiter escape line offset and limit.
   *
   * @see #asXSVLines(InputStream, Charset, int, BiPredicate, String)
   * @param file the file
   * @param offset the offset start from 0, use for skip lines
   * @param limit the max lines to read
   * @param delimiter the field delimiter
   * @return asXSVLines
   */
  public static Stream<List<String>> asXSVLines(final File file, final int offset, final int limit,
      final String delimiter) {
    final FileInputStream fis;
    try {
      fis = new FileInputStream(shouldNotNull(file));
    } catch (FileNotFoundException e1) {
      throw new CorantRuntimeException(e1);
    }
    return asXSVLines(fis, StandardCharsets.UTF_8, offset, (i, t) -> limit >= 1 && i > limit,
        delimiter).onClose(() -> {
          try {
            fis.close();
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        });
  }

  /**
   * Read text file by line and use any character string as a delimiter to split the line into a
   * field list, support delimiter escape.
   *
   * @see #asXSVLines(File, int, int, String)
   * @param file the file
   * @param delimiter the field delimiter
   * @return asXSVLines
   */
  public static Stream<List<String>> asXSVLines(final File file, final String delimiter) {
    return asXSVLines(file, -1, -1, delimiter);
  }

  /**
   * Read the text field by line, and use the delimiter for field splitting; if the field value
   * contains the delimiter, the delimiter in field value must be escaped with a backslash ('\'); if
   * field value contains carriage return ('\r') or newline character ('\n'), also those characters
   * must be escaped with a backslash.
   *
   * NOTE: The caller must maintain resource release by himself.
   *
   * @param is the input stream
   * @param charset
   * @param offset the offset start from 0, use for skip lines
   * @param terminator used to brake out the stream, terminator return true means need to brake out
   * @param delimiter the field delimiter
   */
  public static Stream<List<String>> asXSVLines(final InputStream is, final Charset charset,
      final int offset, final BiPredicate<Integer, String> terminator, final String delimiter) {
    shouldNotNull(delimiter);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
    final Pattern pattern = escapedPattern(Strings.BACK_SLASH, delimiter);
    final String esacpedDelimiter = Strings.BACK_SLASH.concat(delimiter);
    final Function<String, List<String>> converter = line -> {
      ArrayList<String> list = new ArrayList<>(10);
      for (String a : pattern.split(line)) {
        list.add(
            replace(replace(replace(a, esacpedDelimiter, delimiter), XSV_CR_REP, Strings.RETURN),
                XSV_LF_REP, Strings.NEWLINE));
      }
      return list;
    };
    return lines(reader, offset, terminator, converter);
  }

  /**
   * Read by line and use any character string as a delimiter to split the line into a field list,
   * support delimiter escape.
   *
   * NOTE: The caller must maintain resource release by himself.
   *
   * @see #asXSVLines(InputStream, Charset, int, BiPredicate, String)
   *
   * @param is the input stream
   * @param delimiter the field delimiter
   * @return asXSVLines
   */
  public static Stream<List<String>> asXSVLines(final InputStream is, final String delimiter) {
    return asXSVLines(is, StandardCharsets.UTF_8, 0, null, delimiter);
  }

  /**
   * Convert input stream to string
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is
   * @return
   * @throws IOException fromInputStream
   */
  public static String fromInputStream(final InputStream is) throws IOException {
    return fromInputStream(is, StandardCharsets.UTF_8);
  }

  /**
   * Convert input stream to string
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the input stream
   * @param charset
   * @return
   * @throws IOException fromInputStream
   */
  public static String fromInputStream(final InputStream is, final Charset charset)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(is, charset))) {
      int c;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
      }
    }
    return sb.toString();
  }

  public static <T> Stream<T> lines(final BufferedReader reader, final int offset,
      final BiPredicate<Integer, String> terminator, final Function<String, T> converter) {
    return streamOf(new Iterator<>() {
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
      public T next() {
        if (nextLine != null || hasNext()) {
          String line = nextLine;
          nextLine = null;
          return converter.apply(line);
        } else {
          throw new NoSuchElementException();
        }
      }
    });
  }

  /**
   * String lines from file, use for read text file line by line.
   *
   * @param file the text file
   * @return lines
   */
  public static Stream<String> lines(final File file) {
    return lines(file, -1, -1);
  }

  /**
   * String lines from file, use for read text line by line.
   *
   *
   * @param file the file to read
   * @param offset used to skip lines, the offset start from 0
   * @param limit the number of lines returned
   */
  public static Stream<String> lines(final File file, int offset, int limit) {
    final FileInputStream fis;
    try {
      fis = new FileInputStream(shouldNotNull(file));
    } catch (FileNotFoundException e1) {
      throw new CorantRuntimeException(e1);
    }
    return lines(fis, offset, limit).onClose(() -> {
      try {
        fis.close();
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  /**
   * String lines from input stream, use for read text file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the text input stream
   */
  public static Stream<String> lines(final InputStream is) {
    return lines(new InputStreamReader(is, StandardCharsets.UTF_8), 0, null);
  }

  /**
   * String lines from input stream, use for read text line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the text input stream
   * @param charset the charset
   * @param offset used to skip lines, the offset start from 0
   * @param terminator used to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStream is, final Charset charset, final int offset,
      final BiPredicate<Integer, String> terminator) {
    return lines(new InputStreamReader(is, charset), offset, terminator);
  }

  /**
   * String lines from input stream, use for read text file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the text input stream
   * @param offset used to skip lines, the offset start from 0
   * @param terminator used to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStream is, final int offset,
      final BiPredicate<Integer, String> terminator) {
    return lines(new InputStreamReader(is, StandardCharsets.UTF_8), offset, terminator);
  }

  /**
   * String lines from input stream, use for read text file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is the text file input stream
   * @param offset the offset start from 0
   * @param limit the number of lines returned
   */
  public static Stream<String> lines(final InputStream is, final int offset, final int limit) {
    return lines(new InputStreamReader(is, StandardCharsets.UTF_8), offset,
        (i, t) -> limit >= 1 && i > limit);
  }

  /**
   * String lines from input stream reader, use for read text file line by line.
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param isr the text input stream reader
   * @param offset the offset start from 0
   * @param terminator used to brake out the stream, terminator return true means need to brake out
   */
  public static Stream<String> lines(final InputStreamReader isr, final int offset,
      final BiPredicate<Integer, String> terminator) {
    final BufferedReader reader = new BufferedReader(isr);
    return lines(reader, offset, terminator, UnaryOperator.identity());
  }

  /**
   * Sting lines from file path.
   *
   * @param filePath the text file path
   * @return lines
   */
  public static Stream<String> lines(final String filePath) {
    return lines(new File(filePath));
  }

  /**
   * Parse CSV line to field list
   *
   * NOTE: Some codes come from com.sun.tools.jdeprscan.CSV, if there is infringement, please inform
   * me(finesoft@gmail.com).
   *
   * @param line the CSV format line
   * @return the CSV fields
   */
  public static List<String> readCSVFields(final String line) {
    List<String> result = new ArrayList<>();
    if (line != null) {
      StringBuilder buf = new StringBuilder();
      int len = line.length();
      byte state = 0; // 0:start,1:in field,2:in field quote,3:end field quote
      for (int i = 0; i < len; i++) {
        char c = line.charAt(i);
        switch (c) {
          case CSV_FIELD_DELIMITER:
            switch (state) {
              case 2:
                buf.append(CSV_FIELD_DELIMITER);
                break;
              default:
                result.add(buf.toString());
                buf.setLength(0);
                state = 0;
                break;
            }
            break;
          case CSV_FIELD_QUOTES:
            switch (state) {
              case 0:
                state = 2;
                break;
              case 2:
                state = 3;
                break;
              case 1:
                throw new IllegalArgumentException(
                    String.format("Unexpected csv quote, line: [%s] char at: [%d]", line, i));
              case 3:
                buf.append(CSV_FIELD_QUOTES);
                state = 2;
                break;
            }
            break;
          default:
            switch (state) {
              case 0:
                state = 1;
                break;
              case 1:
              case 2:
                break;
              case 3:
                throw new IllegalArgumentException(String.format(
                    "Extra csv character after quoted string, line: [%s] char at: [%d]", line, i));
            }
            buf.append(c);
            break;
        }
      }
      if (state == 2) {
        throw new IllegalArgumentException(
            String.format("Unclosed csv quote, line: [%s] length: [%d]", line, line.length()));
      }
      result.add(buf.toString());
    }
    return result;
  }

  /**
   * Return string lines from file path.
   *
   * @param path
   * @return readFromFile
   */
  public static List<String> readLines(final String path) {
    return Texts.lines(new File(path)).collect(Collectors.toList());
  }

  /**
   * Format objects to CSV line string.
   *
   * @param objects
   * @return toCSVLine
   */
  public static String toCSVLine(final Iterable<?> objects) {
    String line = streamOf(objects).map(o -> Objects.asString(o, EMPTY)).map(s -> {
      boolean q =
          s.contains(CSV_FIELD_DELIMITER_STRING) || s.contains(RETURN) || s.contains(NEWLINE);
      String r;
      if (s.contains(CSV_FIELD_QUOTES_STRING)) {
        q = true;
        r = s.replace(CSV_FIELD_QUOTES_STRING, CSV_DOUBLE_QUOTES);
      } else {
        r = s;
      }
      return q ? CSV_FIELD_QUOTES_STRING + r + CSV_FIELD_QUOTES_STRING : r;
    }).collect(Collectors.joining(CSV_FIELD_DELIMITER_STRING));
    return line.endsWith(String.valueOf(CSV_FIELD_DELIMITER)) ? line.substring(0, line.length() - 1)
        : line;
  }

  /**
   * Format objects to CSV line string.
   *
   * @param objects
   * @return toCSVLine
   */
  public static String toCSVLine(Object... objects) {
    return toCSVLine(listOf(objects));
  }

  /**
   * Format objects to XSV line string.
   *
   * @param objects
   * @param delimiter
   * @return toXSVLine
   */
  public static String toXSVLine(final Iterable<?> objects, String delimiter) {
    final String re = Strings.BACK_SLASH.concat(shouldNotEmpty(delimiter));
    return streamOf(objects).map(o -> Objects.asString(o, EMPTY))
        .map(o -> replace(
            replace(replace(o, Strings.NEWLINE, XSV_LF_REP), Strings.RETURN, XSV_CR_REP), delimiter,
            re))
        .collect(Collectors.joining(delimiter));
  }

  /**
   * Convert input stream to String
   *
   * Note: The caller must maintain resource release by himself
   *
   * @param is
   * @return tryFromInputStream
   */
  public static String tryFromInputStream(final InputStream is) {
    try {
      return fromInputStream(is);
    } catch (IOException e) {
      return null;
    }
  }

  public static void tryWriteToFile(File file, boolean append, Charset charset, String data) {
    try (OutputStream os = new FileOutputStream(file, append);
        BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(os, charset))) {
      fileWriter.append(data);
    } catch (IOException e) {
      // Noop!
    }
  }

  public static void tryWriteToFile(File file, boolean append, String data) {
    try (OutputStream os = new FileOutputStream(file, append);
        BufferedWriter fileWriter =
            new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
      fileWriter.append(data);
    } catch (IOException e) {
      // Noop!
    }
  }

  public static void tryWriteToFile(File file, Iterable<String> data) {
    try {
      writeToFile(file, false, streamOf(data));
    } catch (IOException e) {
      // Noop!
    }
  }

  public static void tryWriteToFile(File file, Stream<String> data) {
    try {
      writeToFile(file, false, data);
    } catch (IOException e) {
      // Noop!
    }
  }

  public static void writeCSVFile(File file, boolean append, Charset charset,
      Stream<List<String>> stream) throws IOException {
    writeToFile(file, append, charset, shouldNotNull(stream).map(Texts::toCSVLine));
  }

  public static void writeCSVFile(File file, List<List<String>> data) throws IOException {
    writeToFile(file, false, shouldNotNull(streamOf(data)).map(Texts::toCSVLine));
  }

  public static void writeToFile(File file, boolean append, Charset charset, Stream<String> lines)
      throws IOException {
    shouldNotNull(lines);
    if (!file.exists()) {
      shouldBeTrue(file.createNewFile());
    }
    try (OutputStream os = new FileOutputStream(file, append);
        BufferedWriter fileWriter = new BufferedWriter(
            new OutputStreamWriter(os, defaultObject(charset, StandardCharsets.UTF_8)))) {
      lines.forEach(line -> {
        try {
          fileWriter.append(line);
          fileWriter.newLine();
          fileWriter.flush();
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

  /**
   * Write string to file line by line, string lines will be written to the beginning of the file.
   *
   * @param file
   * @param data
   * @throws IOException writeToFile
   */
  public static void writeToFile(File file, Iterable<String> data) throws IOException {
    writeToFile(file, false, streamOf(data));
  }

  public static void writeXSVFile(File file, boolean append, Charset charset, String delimiter,
      Stream<List<String>> stream) throws IOException {
    writeToFile(file, append, charset, shouldNotNull(stream).map(s -> toXSVLine(s, delimiter)));
  }

  /**
   * corant-shared
   *
   * Special reader, used to deal with CSV line breaks.
   *
   * @author bingo 上午11:00:21
   *
   */
  static class CSVBufferedReader extends BufferedReader {

    public CSVBufferedReader(Reader in) {
      super(in);
    }

    @Override
    public String readLine() throws IOException {
      return readCSVLine();
    }

    String readCSVLine() throws IOException {
      StringBuilder result = new StringBuilder(128);
      boolean inquotes = false;
      for (;;) {
        int intRead = read();
        if (intRead == -1) {
          return result.length() == 0 ? null : result.toString();
        }
        char c = (char) intRead;
        if (c == CSV_FIELD_QUOTES) {
          inquotes = !inquotes;
        }
        if (!inquotes) {
          if (c == Chars.NEWLINE) {
            break;
          } else if (c == Chars.RETURN) {
            if (markSupported()) {
              mark(1);// for windows excel '\r\n' check if next is '\n' then skip it
              if ((char) read() != Chars.NEWLINE) {
                reset();
              }
            }
            break;
          }
        }
        result.append(c);
      }
      return result.toString();
    }

  }
}
