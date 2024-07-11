/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mongodb;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Sets.setOf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.corant.modules.json.ObjectMappers;
import org.corant.shared.ubiquity.Tuple;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Quartet;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import junit.framework.TestCase;

/**
 * corant-modules-mongodb
 *
 * @author bingo 10:40:07
 */
public class MongoTemplateTest extends TestCase {

  static String murl = "mongodb://**:******@localhost:27017/?authMechanism=SCRAM-SHA-1";

  @Test
  public void testConvert() throws JsonProcessingException {
    MapPojo pojo = new MapPojo();
    Document doc = new Document(ObjectMappers.toDocMap(pojo));
    doc.put("$gt", Arrays.asList(799123L, 123123L));
    BsonDocument bsonDoc =
        doc.toBsonDocument(BsonDocument.class, MongoTemplate.DEFAULT_CODEC_REGISTRY);
    System.out.println(bsonDoc);
    System.out.println(MongoExtendedJsons.toBson(pojo, true));
  }

  @Test
  public void testDelete() {
    MongoTemplate mt = new MongoTemplate(Mongos.resolveClient(murl).getDatabase("anncy"));
    mt.delete("save-many-test", 1);
    mt.deleteMany("save-many-test", mapOf("_id", mapOf("$lt", 10)));
  }

  @Test
  public void testDocuemntConvert() throws JsonProcessingException {
    MapPojo pojo = new MapPojo();
    Document doc = new Document(ObjectMappers.toDocMap(pojo));
    doc.put("_id", doc.remove("id"));
    System.out.println(doc);
    MapPojo pojox = Mongos.resolveEntity(doc, MapPojo.class);
    System.out.println(areEqual(pojo, pojox));
  }

  @Test
  public void testFilter() {
    MongoClient mc = Mongos.resolveClient(murl);
    MongoDatabase md = mc.getDatabase("parts360-data");
    Map<String, Object> filter = mapOf("$and", listOf(mapOf("type.id", 147169256310571008L),
        mapOf("refreshedTime", mapOf("$gt", LocalDate.of(2023, 11, 27)))));
    MongoTemplate mt = new MongoTemplate(md);
    mt.query().collectionName("ct_part").filterMap(filter).limit(100).find()
        .forEach(System.out::println);

  }

  @Test
  public void testInsert() {
    MongoTemplate mt = new MongoTemplate(Mongos.resolveClient(murl).getDatabase("anncy"));
    mt.insert("bingo", new MapPojo());
    mt.insertMany("bingo", IntStream.range(1, 100).mapToObj(MapPojo::new).toList());
  }

  @Test
  public void testPrimaryArray() {
    MongoClient mc = Mongos.resolveClient(murl);
    MongoDatabase md = mc.getDatabase("anncy");
    MongoTemplate mt = new MongoTemplate(md);
    MapPojo pojo = new MapPojo();
    mt.save("bingo", pojo);
    MapPojo x = ObjectMappers.fromMap(
        mt.query().collectionName("bingo").filterMap(mapOf("_id", pojo.getId())).findOne(),
        MapPojo.class);
    assertEquals(pojo, x);
  }

  @Test
  public void testSaveMany() {
    List<Map<Object, Object>> list = new ArrayList<>();
    IntStream.range(0, 100).mapToObj(i -> mapOf("id", i, "name", "name-" + i, "entry",
        mapOf("key", "key-" + i, "value", "value-" + i))).forEach(list::add);
    MongoClient mc = Mongos.resolveClient(murl);
    MongoDatabase md = mc.getDatabase("anncy");
    MongoTemplate mt = new MongoTemplate(md);
    mt.saveMany("save-many-test", list);
    list.forEach(e -> {
      if (getMapInteger(e, "id") < 30) {
        getMapMap(e, "entry").put("uuid",
            getMapString(e, "id") + "-" + UUID.randomUUID().toString());
      }
    });
    mt.saveMany("save-many-test", list);
  }

  @Test
  public void testTuples() {
    TuplePojo pojo = new TuplePojo();
    pojo.setSinglePair(Pair.of("single", 91L));
    pojo.setSingleQuartet(Tuple.quartetOf("SQA", "SQB", 123211, "4.98F"));
    pojo.setSingleTriple(Tuple.tripleOf("sQB", 1231, "0.98F"));
    pojo.setListPairs(listOf(Pair.of("a", 1L), Pair.of("b", 2L), Pair.of("c", 91231239123L)));
    pojo.setListQuartets(listOf(Tuple.quartetOf("QA", "QB", 1, "0.98F"),
        Tuple.quartetOf("QAA", "QBB", 11, "5.18F")));
    pojo.setListTriples(
        listOf(Tuple.tripleOf("QB", 1, "0.98F"), Tuple.tripleOf("QBB", 11, "5.18F")));

    MongoClient mc = Mongos.resolveClient(murl);
    MongoDatabase md = mc.getDatabase("anncy");
    MongoTemplate mt = new MongoTemplate(md);
    mt.save("tuple", pojo);
    System.out.println(pojo.getId());
    TuplePojo mapPojo = mt.query().collectionName("tuple").filterMap(mapOf("_id", pojo.getId()))
        .findOneAs(TuplePojo.class);
    System.out.println(pojo.equals(mapPojo));
  }

  @Test
  public void testTyped() {
    MongoClient mc = Mongos.resolveClient(murl);
    MongoDatabase md = mc.getDatabase("anncy");
    MongoTemplate mt = new MongoTemplate(md);
    CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(new IntegerCodec(), new PowerStatusCodec()),
        CodecRegistries.fromProviders(new MonolightCodecProvider()),
        MongoClientSettings.getDefaultCodecRegistry());
    Monolight myMonolight = new Monolight();
    myMonolight.setPowerStatus(PowerStatus.ON);
    myMonolight.setColorTemperature(5200);
    InsertOneResult ir = mt.executeInsert("typed", myMonolight, codecRegistry, null);
    BsonDocument document = new BsonDocument();
    document.put("_id", ir.getInsertedId());
    Monolight reMonolight = mt.query().collectionName("typed").filter(document)
        .codecRegistry(codecRegistry).findOne(Monolight.class);
    assertEquals(myMonolight, reMonolight);
  }

  @Test
  public void testUpdate() {
    MongoTemplate mt = new MongoTemplate(Mongos.resolveClient(murl).getDatabase("anncy"));
    mt.update("save-many-test", mapOf("_id", 10),
        mapOf("$set", mapOf("entry.bingo", "12312312321")));
    mt.update("save-many-test", mapOf("_id", 10),
        listOf(mapOf("$set", mapOf("entry.bingo0", "ewrwer")),
            mapOf("$set", mapOf("entry.bingo1", "12312312321")),
            mapOf("$set", mapOf("entry.bingo2", "12312312321"))));

    mt.updateMany("save-many-test", mapOf("_id", mapOf("$gt", 80)),
        mapOf("$set", mapOf("entry.bingom", "12312312321")));
    mt.updateMany("save-many-test", mapOf("_id", mapOf("$lt", 15)),
        listOf(mapOf("$set", mapOf("entry.bingom0", "ewrwer")),
            mapOf("$set", mapOf("entry.bingom1", "12312312321")),
            mapOf("$set", mapOf("entry.bingom2", "12312312321"))));
  }

  static class MapEntry {
    byte[] byteArray = {1, 2, 3};
    int[] intArray = {1, 2, 3};
    short[] shortArray = {1, 2, 3};
    Set<Instant> instantSet = setOf(Instant.now(), Instant.now());

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MapEntry other = (MapEntry) obj;
      if (!Arrays.equals(byteArray, other.byteArray)) {
        return false;
      }
      if (instantSet == null) {
        if (other.instantSet != null) {
          return false;
        }
      } else if (!instantSet.equals(other.instantSet)) {
        return false;
      }
      if (!Arrays.equals(intArray, other.intArray)) {
        return false;
      }
      if (!Arrays.equals(shortArray, other.shortArray)) {
        return false;
      }
      return true;
    }

    public byte[] getByteArray() {
      return byteArray;
    }

    public Set<Instant> getInstantSet() {
      return instantSet;
    }

    public int[] getIntArray() {
      return intArray;
    }

    public short[] getShortArray() {
      return shortArray;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(byteArray);
      result = prime * result + ((instantSet == null) ? 0 : instantSet.hashCode());
      result = prime * result + Arrays.hashCode(intArray);
      return prime * result + Arrays.hashCode(shortArray);
    }

  }

  static class MapPojo {
    Long id = System.currentTimeMillis();
    byte[] byteArray = {1, 2, 3};
    int[] intArray = {1, 2, 3};
    short[] shortArray = {1, 2, 3};
    long[] longArray = {1, 2, 3};
    float[] floatArray = {1.0f, 2.0f, 3.0f};
    double[] doubleArray = {1.0, 2.0, 3.0};
    boolean[] booleanArray = {true, false, true};

    byte byteValue = Byte.MAX_VALUE;
    int intValue = Integer.MAX_VALUE;
    short shortValue = Short.MAX_VALUE;
    long longValue = Long.MAX_VALUE;
    float floatValue = Float.MAX_VALUE;
    double doubleValue = Double.MAX_VALUE;
    boolean booleanValue = true;

    BigDecimal bigDecimalValue = BigDecimal.TEN;
    BigInteger bigIntegerValue = bigDecimalValue.toBigInteger();

    URL urlValue;
    URI uriValue;

    Instant instantValue = Instant.now();

    Instant[] instantArray = {Instant.now(), Instant.now(), Instant.now(), Instant.now()};
    String[] stringArray = {"1", "2", "3"};
    List<String> stringList = listOf("1", "2", "3");
    Set<String> stringSet = setOf("1", "2", "3");
    Set<MapEntry> entriesSet = setOf(new MapEntry(), new MapEntry());
    Pair<MapEntry, MapEntry> xxxx = Pair.of(new MapEntry(), new MapEntry());
    Triple<String, String, MapEntry> yyyy = Triple.of("ttt", "aaa", new MapEntry());
    Pair<String, Pair<MapEntry, MapEntry>> zzz =
        Pair.of("hoho", Pair.of(new MapEntry(), new MapEntry()));

    MapPojo() {
      try {
        urlValue = new URL("http://www.corant.org");
        uriValue = urlValue.toURI();
      } catch (Exception e) {
      }
    }

    MapPojo(long id) {
      this.id = id;
      try {
        urlValue = new URL("http://www.corant.org");
        uriValue = urlValue.toURI();
      } catch (Exception e) {
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MapPojo other = (MapPojo) obj;
      return Objects.equals(bigDecimalValue, other.bigDecimalValue)
          && Objects.equals(bigIntegerValue, other.bigIntegerValue)
          && Arrays.equals(booleanArray, other.booleanArray) && booleanValue == other.booleanValue
          && Arrays.equals(byteArray, other.byteArray) && byteValue == other.byteValue
          && Arrays.equals(doubleArray, other.doubleArray)
          && Double.doubleToLongBits(doubleValue) == Double.doubleToLongBits(other.doubleValue)
          && Objects.equals(entriesSet, other.entriesSet)
          && Arrays.equals(floatArray, other.floatArray)
          && Float.floatToIntBits(floatValue) == Float.floatToIntBits(other.floatValue)
          && Objects.equals(id, other.id) && Arrays.equals(instantArray, other.instantArray)
          && Objects.equals(instantValue, other.instantValue)
          && Arrays.equals(intArray, other.intArray) && intValue == other.intValue
          && Arrays.equals(longArray, other.longArray) && longValue == other.longValue
          && Arrays.equals(shortArray, other.shortArray) && shortValue == other.shortValue
          && Arrays.equals(stringArray, other.stringArray)
          && Objects.equals(stringList, other.stringList)
          && Objects.equals(stringSet, other.stringSet) && Objects.equals(uriValue, other.uriValue)
          && Objects.equals(urlValue, other.urlValue) && Objects.equals(xxxx, other.xxxx)
          && Objects.equals(yyyy, other.yyyy) && Objects.equals(zzz, other.zzz);
    }

    public BigDecimal getBigDecimalValue() {
      return bigDecimalValue;
    }

    public BigInteger getBigIntegerValue() {
      return bigIntegerValue;
    }

    public boolean[] getBooleanArray() {
      return booleanArray;
    }

    public byte[] getByteArray() {
      return byteArray;
    }

    public byte getByteValue() {
      return byteValue;
    }

    public double[] getDoubleArray() {
      return doubleArray;
    }

    public double getDoubleValue() {
      return doubleValue;
    }

    public Set<MapEntry> getEntriesSet() {
      return entriesSet;
    }

    public float[] getFloatArray() {
      return floatArray;
    }

    public float getFloatValue() {
      return floatValue;
    }

    public Long getId() {
      return id;
    }

    public Instant[] getInstantArray() {
      return instantArray;
    }

    public Instant getInstantValue() {
      return instantValue;
    }

    public int[] getIntArray() {
      return intArray;
    }

    public int getIntValue() {
      return intValue;
    }

    public long[] getLongArray() {
      return longArray;
    }

    public long getLongValue() {
      return longValue;
    }

    public short[] getShortArray() {
      return shortArray;
    }

    public short getShortValue() {
      return shortValue;
    }

    public String[] getStringArray() {
      return stringArray;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public Set<String> getStringSet() {
      return stringSet;
    }

    public URI getUriValue() {
      return uriValue;
    }

    public URL getUrlValue() {
      return urlValue;
    }

    public Pair<MapEntry, MapEntry> getXxxx() {
      return xxxx;
    }

    public Triple<String, String, MapEntry> getYyyy() {
      return yyyy;
    }

    public Pair<String, Pair<MapEntry, MapEntry>> getZzz() {
      return zzz;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(booleanArray);
      result = prime * result + Arrays.hashCode(byteArray);
      result = prime * result + Arrays.hashCode(doubleArray);
      result = prime * result + Arrays.hashCode(floatArray);
      result = prime * result + Arrays.hashCode(instantArray);
      result = prime * result + Arrays.hashCode(intArray);
      result = prime * result + Arrays.hashCode(longArray);
      result = prime * result + Arrays.hashCode(shortArray);
      result = prime * result + Arrays.hashCode(stringArray);
      return prime * result + Objects.hash(bigDecimalValue, bigIntegerValue, booleanValue,
          byteValue, doubleValue, entriesSet, floatValue, id, instantValue, intValue, longValue,
          shortValue, stringList, stringSet, uriValue, urlValue, xxxx, yyyy, zzz);
    }

    public boolean isBooleanValue() {
      return booleanValue;
    }

  }

  static class Monolight {
    private PowerStatus powerStatus = PowerStatus.OFF;
    private Integer colorTemperature;

    public Monolight() {}

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Monolight other = (Monolight) obj;
      return Objects.equals(colorTemperature, other.colorTemperature)
          && powerStatus == other.powerStatus;
    }

    public Integer getColorTemperature() {
      return colorTemperature;
    }

    public PowerStatus getPowerStatus() {
      return powerStatus;
    }

    @Override
    public int hashCode() {
      return Objects.hash(colorTemperature, powerStatus);
    }

    public void setColorTemperature(Integer colorTemperature) {
      this.colorTemperature = colorTemperature;
    }

    public void setPowerStatus(PowerStatus powerStatus) {
      this.powerStatus = powerStatus;
    }

  }

  static class MonolightCodec implements Codec<Monolight> {
    private Codec<PowerStatus> powerStatusCodec;
    private Codec<Integer> integerCodec;

    public MonolightCodec(CodecRegistry registry) {
      powerStatusCodec = registry.get(PowerStatus.class);
      integerCodec = registry.get(Integer.class);
    }

    // Defines a decode() method to convert BSON values to Monolight enum values
    @Override
    public Monolight decode(BsonReader reader, DecoderContext decoderContext) {
      Monolight monolight = new Monolight();
      reader.readStartDocument();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        String fieldName = reader.readName();
        if ("powerStatus".equals(fieldName)) {
          monolight.setPowerStatus(powerStatusCodec.decode(reader, decoderContext));
        } else if ("colorTemperature".equals(fieldName)) {
          monolight.setColorTemperature(integerCodec.decode(reader, decoderContext));
        } else if ("_id".equals(fieldName)) {
          reader.readObjectId();
        }
      }
      reader.readEndDocument();
      return monolight;
    }

    // Defines an encode() method to convert Monolight enum values to BSON values
    @Override
    public void encode(BsonWriter writer, Monolight value, EncoderContext encoderContext) {
      writer.writeStartDocument();
      writer.writeName("powerStatus");
      powerStatusCodec.encode(writer, value.getPowerStatus(), encoderContext);
      writer.writeName("colorTemperature");
      integerCodec.encode(writer, value.getColorTemperature(), encoderContext);
      writer.writeEndDocument();
    }

    // Returns an instance of the Monolight class, since Java cannot infer the class type
    @Override
    public Class<Monolight> getEncoderClass() {
      return Monolight.class;
    }
  }

  static class MonolightCodecProvider implements CodecProvider {
    public MonolightCodecProvider() {}

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
      if (clazz == Monolight.class) {
        return (Codec<T>) new MonolightCodec(registry);
      }
      // return null when not a provider for the requested class
      return null;
    }
  }

  enum PowerStatus {
    ON, OFF
  }

  static class PowerStatusCodec implements Codec<PowerStatus> {
    @Override
    public PowerStatus decode(BsonReader reader, DecoderContext decoderContext) {
      return reader.readBoolean() ? PowerStatus.ON : PowerStatus.OFF;
    }

    @Override
    public void encode(BsonWriter writer, PowerStatus value, EncoderContext encoderContext) {
      if (value != null) {
        writer.writeBoolean(PowerStatus.ON.equals(value) ? true : false);
      }
    }

    @Override
    public Class<PowerStatus> getEncoderClass() {
      return PowerStatus.class;
    }
  }

  static class TuplePojo {

    Long id = System.currentTimeMillis();

    Pair<String, Long> singlePair;
    List<Pair<String, Long>> listPairs;

    Quartet<String, String, Integer, String> singleQuartet;
    List<Quartet<String, String, Integer, String>> listQuartets;

    Triple<String, Integer, String> singleTriple;
    List<Triple<String, Integer, String>> listTriples;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TuplePojo other = (TuplePojo) obj;
      return Objects.equals(listPairs, other.listPairs)
          && Objects.equals(listQuartets, other.listQuartets)
          && Objects.equals(listTriples, other.listTriples)
          && Objects.equals(singlePair, other.singlePair)
          && Objects.equals(singleQuartet, other.singleQuartet)
          && Objects.equals(singleTriple, other.singleTriple);
    }

    public Long getId() {
      return id;
    }

    public List<Pair<String, Long>> getListPairs() {
      return listPairs;
    }

    public List<Quartet<String, String, Integer, String>> getListQuartets() {
      return listQuartets;
    }

    public List<Triple<String, Integer, String>> getListTriples() {
      return listTriples;
    }

    public Pair<String, Long> getSinglePair() {
      return singlePair;
    }

    public Quartet<String, String, Integer, String> getSingleQuartet() {
      return singleQuartet;
    }

    public Triple<String, Integer, String> getSingleTriple() {
      return singleTriple;
    }

    @Override
    public int hashCode() {
      return Objects.hash(listPairs, listQuartets, listTriples, singlePair, singleQuartet,
          singleTriple);
    }

    public void setId(Long id) {
      this.id = id;
    }

    public void setListPairs(List<Pair<String, Long>> listPairs) {
      this.listPairs = listPairs;
    }

    public void setListQuartets(List<Quartet<String, String, Integer, String>> listQuartets) {
      this.listQuartets = listQuartets;
    }

    public void setListTriples(List<Triple<String, Integer, String>> listTriples) {
      this.listTriples = listTriples;
    }

    public void setSinglePair(Pair<String, Long> singlePair) {
      this.singlePair = singlePair;
    }

    public void setSingleQuartet(Quartet<String, String, Integer, String> singleQuartet) {
      this.singleQuartet = singleQuartet;
    }

    public void setSingleTriple(Triple<String, Integer, String> singleTriple) {
      this.singleTriple = singleTriple;
    }

  }
}
