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
package org.corant.modules.json.extensions;

import java.io.IOException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

/**
 * corant-modules-json
 *
 * @author bingo 14:21:09
 */
public class CorantTupleModule extends Module {

  static final String NAME = "CorantTupleModule";

  public static final CorantTupleModule INSTANCE = new CorantTupleModule();

  protected CorantTupleModule() {}

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public String getModuleName() {
    return NAME;
  }

  @Override
  public int hashCode() {
    return NAME.hashCode();
  }

  @Override
  public void setupModule(SetupContext context) {
    SimpleDeserializers desers = new SimpleDeserializers();
    desers.addDeserializer(Pair.class, new PairDeserializer());
    desers.addDeserializer(Range.class, new RangeDeserializer());
    desers.addDeserializer(Triple.class, new TripleDeserializer());
    context.addDeserializers(desers);

    SimpleSerializers sers = new SimpleSerializers();
    sers.addSerializer(Pair.class, new PairSerializer());
    sers.addSerializer(Range.class, new RangeSerializer());
    sers.addSerializer(Triple.class, new TripleSerializer());
    context.addSerializers(sers);
  }

  @Override
  public Version version() {
    return PackageVersion.VERSION;
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:10
   */
  @SuppressWarnings("rawtypes")
  public static class PairDeserializer extends JsonDeserializer<Pair> {
    @Override
    public Pair deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Pair.of(array[0], array[1]);
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:14
   */
  @SuppressWarnings("rawtypes")
  public static class PairSerializer extends JsonSerializer<Pair> {
    @Override
    public void serialize(Pair pair, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(pair, 2);
      gen.writeObject(pair.getLeft());
      gen.writeObject(pair.getRight());
      gen.writeEndArray();
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:10
   */
  @SuppressWarnings("rawtypes")
  public static class RangeDeserializer extends JsonDeserializer<Range> {
    @SuppressWarnings("unchecked")
    @Override
    public Range deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Range.of((Comparable) array[0], (Comparable) array[1]);
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:14
   */
  @SuppressWarnings("rawtypes")
  public static class RangeSerializer extends JsonSerializer<Range> {
    @Override
    public void serialize(Range range, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(range, 2);
      gen.writeObject(range.getMin());
      gen.writeObject(range.getMax());
      gen.writeEndArray();
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:10
   */
  @SuppressWarnings("rawtypes")
  public static class TripleDeserializer extends JsonDeserializer<Triple> {
    @Override
    public Triple deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Triple.of(array[0], array[1], array[2]);
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 下午12:10:14
   */
  @SuppressWarnings("rawtypes")
  public static class TripleSerializer extends JsonSerializer<Triple> {
    @Override
    public void serialize(Triple triple, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(triple, 3);
      gen.writeObject(triple.getLeft());
      gen.writeObject(triple.getMiddle());
      gen.writeObject(triple.getRight());
      gen.writeEndArray();
    }
  }
}
