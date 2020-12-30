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
  public void testCSVLine() {
    List<String> datLine = listOf("1", "", "2", "3", "\"", "4", "\r", "\n", "5");
    String csvLine = Texts.toCSVLine(datLine);
    assertEquals(datLine, Texts.fromCSVLine(csvLine));
  }

  @Test
  public void testStreamCSVRows() {
    List<String> datLine =
        listOf("1", "", "2", "3", "\"", ",", "\r\n", "\r\r\r\r", "\n\n\r", "4", "\r", "\n", "5");
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
  }
}
