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
package org.corant.shared.util;

import static org.corant.shared.util.Lists.listOf;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 上午10:08:19
 *
 */
public class TextsTest extends TestCase {

  @Test
  public void testStreamCSVRows() throws IOException {
    File testFile = FileUtils.createTempFile("corant-testing-csv-rows", ".csv");
    List<String> datLine = listOf("1", "", "2", "3", "\"", ",", "厦门China", "\r\n", "\r\r\r\r",
        "\n\n\r", "4", "\r", "\n", "5");
    String csvLine = Texts.toCSVLine(datLine);
    int size = 100;
    List<List<String>> datLines = new ArrayList<>(size);
    StringBuilder context = new StringBuilder(csvLine.length() * size + size);
    for (int i = 0; i < size; i++) {
      context.append(csvLine).append(Chars.NEWLINE);
      datLines.add(datLine);
    }
    List<List<String>> results =
        Texts.asCSVLines(Texts.asInputStream(context.toString())).collect(Collectors.toList());
    assertEquals(results, datLines);
    Texts.writeCSVFile(testFile, false, Charset.forName("GB2312"), results.stream());
    List<List<String>> reads = new ArrayList<>();
    Texts.asCSVLines(testFile, Charset.forName("GB2312")).forEach(reads::add);
    assertEquals(results, reads);
    reads.clear();
    Texts.writeCSVFile(testFile, false, Charset.forName("GB2312"), results);
    Texts.asCSVLines(testFile, Charset.forName("GB2312")).forEach(reads::add);
    assertEquals(results, reads);
  }

  @Test
  public void testStreamXSVRows() throws IOException {
    File testFile = FileUtils.createTempFile("corant-testing-xsv-rows", ".xsv");
    List<String> datLine = listOf("1", "", "\\t2", "3", "\"", ",", "厦门\tChina", "\r\n", "\r\r\r\r",
        "\n\n\r", "4", "\r", "\n", "5");
    String delimiter = "\t";
    String xsvLine = Texts.toXSVLine(datLine, delimiter);
    int size = 2;
    List<List<String>> datLines = new ArrayList<>(size);
    StringBuilder context = new StringBuilder(xsvLine.length() * size + size);
    for (int i = 0; i < size; i++) {
      context.append(xsvLine).append(Chars.NEWLINE);
      datLines.add(datLine);
    }
    List<List<String>> results =
        Texts.asXSVLines(Texts.asInputStream(context.toString()), delimiter)
            .collect(Collectors.toList());
    assertEquals(results, datLines);
    Texts.writeXSVFile(testFile, false, null, delimiter, results.stream());
    List<List<String>> reads = new ArrayList<>();
    Texts.asXSVLines(testFile, delimiter).forEach(reads::add);
    assertEquals(results, reads);
    reads.clear();
    Texts.writeXSVFile(testFile, false, null, delimiter, results);
    Texts.asXSVLines(testFile, delimiter).forEach(reads::add);
    assertEquals(results, reads);

  }
}
