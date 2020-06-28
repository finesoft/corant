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

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.mapOf;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.corant.shared.util.Resources.SourceType;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午8:17:00
 *
 */
public class ConversionsTest extends TestCase {

  @Test
  public void testEnum() {
    int idx = 0;
    assertEquals(Conversions.toEnum(idx, SourceType.class), SourceType.FILE_SYSTEM);
    String s = "SourceType.file_system";
    assertEquals(Conversions.toEnum(s, SourceType.class), SourceType.FILE_SYSTEM);
    s = "SourceType.FILE_SYSTEM";
    assertEquals(Conversions.toEnum(s, SourceType.class), SourceType.FILE_SYSTEM);
  }

  @Test
  public void testLocalDate() {
    List<Integer> list = listOf(2020, 6, 28);
    Map<String, Object> map = mapOf("year", 2020, "month", 6, "dayOfMonth", 28);
    assertEquals(Conversions.toLocalDate(list), Conversions.toLocalDate(map));
    assertEquals(Conversions.toLocalDate(list), Conversions.toLocalDate("2020-06-28"));
    List<List<Integer>> lists =
        listOf(listOf(2020, 6, 28), listOf(2020, 6, 28), listOf(2020, 6, 28));
    List<Map<String, Object>> maps = listOf(mapOf("year", 2020, "month", 6, "dayOfMonth", 28),
        mapOf("year", 2020, "month", 6, "dayOfMonth", 28),
        mapOf("year", 2020, "month", 6, "dayOfMonth", 28));
    Conversions.toList(lists, Conversions::toLocalDate)
        .forEach(r -> assertEquals(r, Conversions.toLocalDate("2020-06-28")));
    Conversions.toList(maps, Conversions::toLocalDate)
        .forEach(r -> assertEquals(r, Conversions.toLocalDate("2020-06-28")));
  }

  @Test
  public void testStringObject() {
    String s = "PT15M";
    assertEquals(Conversions.toDuration(s), Duration.ofMinutes(15));
  }
}
