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
package org.corant.modules.json.expression.predicate;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Sets.setOf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.modules.json.ForwardingObjectMappers;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.junit.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

/**
 * corant-modules-json
 *
 * @author bingo 11:18:16
 */
public class JsonsTest extends TestCase {

  @Test
  public void testPojoToMap() {
    ObjectMapper om = ForwardingObjectMappers.objectMapper();
    MapPojo mp = new MapPojo();
    Map<Object, Object> map = om.convertValue(mp, new TypeReference<Map<Object, Object>>() {});
    map.forEach((k, v) -> System.out.println(k + "\t" + v.getClass()));
    MapPojo dmp = om.convertValue(map, MapPojo.class);
    assertEquals(mp, dmp);
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
      if (bigDecimalValue == null) {
        if (other.bigDecimalValue != null) {
          return false;
        }
      } else if (!bigDecimalValue.equals(other.bigDecimalValue)) {
        return false;
      }
      if (bigIntegerValue == null) {
        if (other.bigIntegerValue != null) {
          return false;
        }
      } else if (!bigIntegerValue.equals(other.bigIntegerValue)) {
        return false;
      }
      if (!Arrays.equals(booleanArray, other.booleanArray)) {
        return false;
      }
      if (booleanValue != other.booleanValue) {
        return false;
      }
      if (!Arrays.equals(byteArray, other.byteArray)) {
        return false;
      }
      if (byteValue != other.byteValue) {
        return false;
      }
      if (!Arrays.equals(doubleArray, other.doubleArray)) {
        return false;
      }
      if (Double.doubleToLongBits(doubleValue) != Double.doubleToLongBits(other.doubleValue)) {
        return false;
      }
      if (entriesSet == null) {
        if (other.entriesSet != null) {
          return false;
        }
      } else if (!entriesSet.equals(other.entriesSet)) {
        return false;
      }
      if (!Arrays.equals(floatArray, other.floatArray)) {
        return false;
      }
      if (Float.floatToIntBits(floatValue) != Float.floatToIntBits(other.floatValue)) {
        return false;
      }
      if (!Arrays.equals(instantArray, other.instantArray)) {
        return false;
      }
      if (instantValue == null) {
        if (other.instantValue != null) {
          return false;
        }
      } else if (!instantValue.equals(other.instantValue)) {
        return false;
      }
      if (!Arrays.equals(intArray, other.intArray)) {
        return false;
      }
      if (intValue != other.intValue) {
        return false;
      }
      if (!Arrays.equals(longArray, other.longArray)) {
        return false;
      }
      if (longValue != other.longValue) {
        return false;
      }
      if (!Arrays.equals(shortArray, other.shortArray)) {
        return false;
      }
      if (shortValue != other.shortValue) {
        return false;
      }
      if (!Arrays.equals(stringArray, other.stringArray)) {
        return false;
      }
      if (stringList == null) {
        if (other.stringList != null) {
          return false;
        }
      } else if (!stringList.equals(other.stringList)) {
        return false;
      }
      if (stringSet == null) {
        if (other.stringSet != null) {
          return false;
        }
      } else if (!stringSet.equals(other.stringSet)) {
        return false;
      }
      if (uriValue == null) {
        if (other.uriValue != null) {
          return false;
        }
      } else if (!uriValue.equals(other.uriValue)) {
        return false;
      }
      if (urlValue == null) {
        if (other.urlValue != null) {
          return false;
        }
      } else if (!urlValue.equals(other.urlValue)) {
        return false;
      }
      if (xxxx == null) {
        if (other.xxxx != null) {
          return false;
        }
      } else if (!xxxx.equals(other.xxxx)) {
        return false;
      }
      if (yyyy == null) {
        if (other.yyyy != null) {
          return false;
        }
      } else if (!yyyy.equals(other.yyyy)) {
        return false;
      }
      if (zzz == null) {
        if (other.zzz != null) {
          return false;
        }
      } else if (!zzz.equals(other.zzz)) {
        return false;
      }
      return true;
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
      result = prime * result + ((bigDecimalValue == null) ? 0 : bigDecimalValue.hashCode());
      result = prime * result + ((bigIntegerValue == null) ? 0 : bigIntegerValue.hashCode());
      result = prime * result + Arrays.hashCode(booleanArray);
      result = prime * result + (booleanValue ? 1231 : 1237);
      result = prime * result + Arrays.hashCode(byteArray);
      result = prime * result + byteValue;
      result = prime * result + Arrays.hashCode(doubleArray);
      long temp;
      temp = Double.doubleToLongBits(doubleValue);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      result = prime * result + ((entriesSet == null) ? 0 : entriesSet.hashCode());
      result = prime * result + Arrays.hashCode(floatArray);
      result = prime * result + Float.floatToIntBits(floatValue);
      result = prime * result + Arrays.hashCode(instantArray);
      result = prime * result + ((instantValue == null) ? 0 : instantValue.hashCode());
      result = prime * result + Arrays.hashCode(intArray);
      result = prime * result + intValue;
      result = prime * result + Arrays.hashCode(longArray);
      result = prime * result + (int) (longValue ^ (longValue >>> 32));
      result = prime * result + Arrays.hashCode(shortArray);
      result = prime * result + shortValue;
      result = prime * result + Arrays.hashCode(stringArray);
      result = prime * result + ((stringList == null) ? 0 : stringList.hashCode());
      result = prime * result + ((stringSet == null) ? 0 : stringSet.hashCode());
      result = prime * result + ((uriValue == null) ? 0 : uriValue.hashCode());
      result = prime * result + ((urlValue == null) ? 0 : urlValue.hashCode());
      result = prime * result + ((xxxx == null) ? 0 : xxxx.hashCode());
      result = prime * result + ((yyyy == null) ? 0 : yyyy.hashCode());
      return prime * result + ((zzz == null) ? 0 : zzz.hashCode());
    }

    public boolean isBooleanValue() {
      return booleanValue;
    }

  }
}
