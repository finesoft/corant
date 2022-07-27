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
package org.corant.modules.json.expression.predicate;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapBigDecimal;
import static org.corant.shared.util.Maps.getMapBigInteger;
import static org.corant.shared.util.Maps.getMapDouble;
import static org.corant.shared.util.Maps.getMapEnum;
import static org.corant.shared.util.Maps.getMapFloat;
import static org.corant.shared.util.Maps.getMapInstant;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Maps.getMapList;
import static org.corant.shared.util.Maps.getMapLocalDateTime;
import static org.corant.shared.util.Maps.getMapLong;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapObject;
import static org.corant.shared.util.Maps.getMapShort;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.getMapZonedDateTime;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bson.Document;
import org.corant.modules.bson.Bsons;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.Conversions;
import junit.framework.TestCase;

/**
 * corant-modules-json
 *
 * @author bingo 上午12:06:18
 *
 */
public class BsonsTest extends TestCase {

  static String bson = "1010";
  static Instant instant = Instant.now();
  static byte[] bytes = bson.repeat(100).getBytes();

  public void testList() {
    Document doc = new Document();
    doc.put("bytes", listOf(bytes, bytes));
    doc.put("String", listOf(bson, bson));
    doc.put("Enum", listOf(SourceType.CLASS_PATH, SourceType.CLASS_PATH));
    doc.put("BigDecimal", listOf(new BigDecimal(bson), new BigDecimal(bson)));
    doc.put("BigInteger", listOf(new BigInteger(bson), new BigInteger(bson)));
    doc.put("Long", listOf(Long.valueOf(bson), Long.valueOf(bson)));
    doc.put("Integer", listOf(Integer.valueOf(bson), Integer.valueOf(bson)));
    doc.put("Short", listOf(Short.valueOf(bson), Short.valueOf(bson)));
    doc.put("Double", listOf(Double.valueOf(bson), Double.valueOf(bson)));
    doc.put("Float", listOf(Float.valueOf(bson), Float.valueOf(bson)));
    doc.put("Instant", listOf(instant, instant));
    doc.put("Date", listOf(new Date(instant.toEpochMilli()), new Date(instant.toEpochMilli())));
    doc.put("SQLDate", listOf(new java.sql.Date(instant.toEpochMilli()),
        new java.sql.Date(instant.toEpochMilli())));
    doc.put("ZonedDateTime",
        listOf(instant.atZone(ZoneId.systemDefault()), instant.atZone(ZoneId.systemDefault())));
    doc.put("LocalDateTime", listOf(instant.atZone(ZoneId.of("UTC")).toLocalDateTime(),
        instant.atZone(ZoneId.of("UTC")).toLocalDateTime()));

    byte[] docBytes = Bsons.toBytes(doc);
    Map<String, Object> xdoc = Bsons.fromBytes(docBytes);

    List<Instant> xinstant = getMapList(xdoc, "Instant", Instant.class);
    List<ZonedDateTime> zonedDateTime = getMapList(xdoc, "ZonedDateTime", ZonedDateTime.class);
    List<LocalDateTime> localDateTime =
        getMapList(xdoc, "LocalDateTime", e -> Conversions.toLocalDateTime(e, ZoneId.of("UTC")));

    assertEquals(
        listOf(instant, instant).stream().map(Instant::toEpochMilli).collect(Collectors.toList()),
        xinstant.stream().map(Instant::toEpochMilli).collect(Collectors.toList()));
    assertEquals(zonedDateTime,
        listOf(instant.atZone(ZoneId.systemDefault()), instant.atZone(ZoneId.systemDefault())));

    assertEquals(
        localDateTime.stream().map(e -> e.truncatedTo(ChronoUnit.MILLIS))
            .collect(Collectors.toList()),
        listOf(instant.atZone(ZoneId.of("UTC")).toLocalDateTime(),
            instant.atZone(ZoneId.of("UTC")).toLocalDateTime()).stream()
                .map(e -> e.truncatedTo(ChronoUnit.MILLIS)).collect(Collectors.toList()));

    assertEquals(listOf(new BigDecimal(bson), new BigDecimal(bson)),
        getMapList(xdoc, "BigDecimal", BigDecimal.class));

    assertEquals(listOf(SourceType.CLASS_PATH, SourceType.CLASS_PATH),
        getMapList(xdoc, "Enum", SourceType.class));

    assertEquals(listOf(new BigInteger(bson), new BigInteger(bson)),
        getMapList(xdoc, "BigInteger", BigInteger.class));

    assertEquals(listOf(bson, bson), getMapList(xdoc, "String", String.class));

    assertTrue(areEqual(listOf(Long.valueOf(bson), Long.valueOf(bson)),
        getMapList(xdoc, "Long", Long.class)));

    assertTrue(areEqual(listOf(Integer.valueOf(bson), Integer.valueOf(bson)),
        getMapList(xdoc, "Integer", Integer.class)));

    assertTrue(areEqual(listOf(Short.valueOf(bson), Short.valueOf(bson)),
        getMapList(xdoc, "Short", Short.class)));

    assertTrue(areEqual(listOf(Float.valueOf(bson), Float.valueOf(bson)),
        getMapList(xdoc, "Float", Float.class)));

    assertTrue(areEqual(listOf(Double.valueOf(bson), Double.valueOf(bson)),
        getMapList(xdoc, "Double", Double.class)));

    assertTrue(areEqual(listOf(new Date(instant.toEpochMilli()), new Date(instant.toEpochMilli())),
        getMapList(xdoc, "Date", Date.class)));

    assertTrue(areEqual(
        listOf(new java.sql.Date(instant.toEpochMilli()),
            new java.sql.Date(instant.toEpochMilli())),
        getMapList(xdoc, "SQLDate", java.sql.Date.class)));
    assertTrue(Objects.deepEquals(bytes, getMapList(xdoc, "bytes", byte[].class).get(0)));
    assertTrue(Objects.deepEquals(bytes, getMapList(xdoc, "bytes", byte[].class).get(1)));
  }

  public void testMap() {
    Document doc = new Document();
    doc.put("bytes", mapOf("", bytes));
    doc.put("String", mapOf("", bson));
    doc.put("BigDecimal", mapOf("", new BigDecimal(bson)));
    doc.put("BigInteger", mapOf("", new BigInteger(bson)));
    doc.put("Long", mapOf("", Long.valueOf(bson)));
    doc.put("Integer", mapOf("", Integer.valueOf(bson)));
    doc.put("Short", mapOf("", Short.valueOf(bson)));
    doc.put("Double", mapOf("", Double.valueOf(bson)));
    doc.put("Float", mapOf("", Float.valueOf(bson)));
    doc.put("Instant", mapOf("", instant));
    doc.put("Enum", mapOf("", SourceType.CLASS_PATH));
    doc.put("Date", mapOf("", new Date(instant.toEpochMilli())));
    doc.put("SQLDate", mapOf("", new java.sql.Date(instant.toEpochMilli())));
    doc.put("ZonedDateTime", mapOf("", instant.atZone(ZoneId.systemDefault())));
    doc.put("LocalDateTime", mapOf("", instant.atZone(ZoneId.of("UTC")).toLocalDateTime()));

    byte[] docBytes = Bsons.toBytes(doc);
    Map<String, Object> xdoc = Bsons.fromBytes(docBytes);
    Instant xinstant = getMapInstant(getMapMap(xdoc, "Instant"), "");
    ZonedDateTime zonedDateTime = getMapZonedDateTime(getMapMap(xdoc, "ZonedDateTime"), "");
    LocalDateTime localDateTime =
        getMapLocalDateTime(getMapMap(xdoc, "LocalDateTime"), "", ZoneId.of("UTC"));

    assertEquals(instant.toEpochMilli(), xinstant.toEpochMilli());
    assertEquals(zonedDateTime, instant.atZone(ZoneId.systemDefault()));
    assertEquals(localDateTime.truncatedTo(ChronoUnit.MILLIS),
        instant.atZone(ZoneId.of("UTC")).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(new BigDecimal(bson), getMapBigDecimal(getMapMap(xdoc, "BigDecimal"), ""));
    assertEquals(new BigInteger(bson), getMapBigInteger(getMapMap(xdoc, "BigInteger"), ""));
    assertEquals(bson, getMapString(getMapMap(xdoc, "String"), ""));
    assertTrue(areEqual(Long.valueOf(bson), getMapLong(getMapMap(xdoc, "Long"), "")));
    assertTrue(areEqual(Integer.valueOf(bson), getMapInteger(getMapMap(xdoc, "Integer"), "")));
    assertTrue(areEqual(Short.valueOf(bson), getMapShort(getMapMap(xdoc, "Short"), "")));
    assertTrue(areEqual(Float.valueOf(bson), getMapFloat(getMapMap(xdoc, "Float"), "")));
    assertTrue(areEqual(Double.valueOf(bson), getMapDouble(getMapMap(xdoc, "Double"), "")));
    assertEquals(SourceType.CLASS_PATH, getMapEnum(getMapMap(xdoc, "Enum"), "", SourceType.class));
    assertTrue(areEqual(new Date(instant.toEpochMilli()),
        getMapObject(getMapMap(xdoc, "Date"), "", Date.class)));
    assertTrue(areEqual(new java.sql.Date(instant.toEpochMilli()),
        getMapObject(getMapMap(xdoc, "SQLDate"), "", java.sql.Date.class)));
    assertTrue(Objects.deepEquals(bytes, getMapObject(getMapMap(xdoc, "bytes"), "", byte[].class)));
  }

  public void testSingle() {
    Document doc = new Document();
    doc.put("bytes", bytes);
    doc.put("String", bson);
    doc.put("BigDecimal", new BigDecimal(bson));
    doc.put("BigInteger", new BigInteger(bson));
    doc.put("Long", Long.valueOf(bson));
    doc.put("Integer", Integer.valueOf(bson));
    doc.put("Short", Short.valueOf(bson));
    doc.put("Double", Double.valueOf(bson));
    doc.put("Float", Float.valueOf(bson));
    doc.put("Instant", instant);
    doc.put("Date", new Date(instant.toEpochMilli()));
    doc.put("Enum", SourceType.CLASS_PATH);
    doc.put("SQLDate", new java.sql.Date(instant.toEpochMilli()));
    doc.put("ZonedDateTime", instant.atZone(ZoneId.systemDefault()));
    doc.put("LocalDateTime", instant.atZone(ZoneId.of("UTC")).toLocalDateTime());
    byte[] docBytes = Bsons.toBytes(doc);
    Map<String, Object> xdoc = Bsons.fromBytes(docBytes);
    Instant xinstant = getMapInstant(xdoc, "Instant");
    ZonedDateTime zonedDateTime = getMapZonedDateTime(xdoc, "ZonedDateTime");
    LocalDateTime localDateTime = getMapLocalDateTime(xdoc, "LocalDateTime", ZoneId.of("UTC"));
    assertEquals(instant.toEpochMilli(), xinstant.toEpochMilli());
    assertEquals(zonedDateTime, instant.atZone(ZoneId.systemDefault()));
    assertEquals(localDateTime.truncatedTo(ChronoUnit.MILLIS),
        instant.atZone(ZoneId.of("UTC")).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(new BigDecimal(bson), getMapBigDecimal(xdoc, "BigDecimal"));
    assertEquals(new BigInteger(bson), getMapBigInteger(xdoc, "BigInteger"));
    assertEquals(bson, getMapString(xdoc, "String"));
    assertEquals(SourceType.CLASS_PATH, getMapEnum(xdoc, "Enum", SourceType.class));
    assertTrue(areEqual(Long.valueOf(bson), getMapLong(xdoc, "Long")));
    assertTrue(areEqual(Integer.valueOf(bson), getMapInteger(xdoc, "Integer")));
    assertTrue(areEqual(Short.valueOf(bson), getMapShort(xdoc, "Short")));
    assertTrue(areEqual(Float.valueOf(bson), getMapFloat(xdoc, "Float")));
    assertTrue(areEqual(Double.valueOf(bson), getMapDouble(xdoc, "Double")));
    assertTrue(areEqual(new Date(instant.toEpochMilli()), getMapObject(xdoc, "Date", Date.class)));
    assertTrue(areEqual(new java.sql.Date(instant.toEpochMilli()),
        getMapObject(xdoc, "SQLDate", java.sql.Date.class)));
    assertTrue(Objects.deepEquals(bytes, getMapObject(xdoc, "bytes", byte[].class)));
  }
}
