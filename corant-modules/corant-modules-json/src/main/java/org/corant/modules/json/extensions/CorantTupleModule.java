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
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Tuple;
import org.corant.shared.ubiquity.Tuple.Dectet;
import org.corant.shared.ubiquity.Tuple.Duet;
import org.corant.shared.ubiquity.Tuple.Nonet;
import org.corant.shared.ubiquity.Tuple.Octet;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Quartet;
import org.corant.shared.ubiquity.Tuple.Quintet;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Septet;
import org.corant.shared.ubiquity.Tuple.Sextet;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.ubiquity.Tuple.Triplet;
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
    desers.addDeserializer(Pair.class, new TupleDeserializer<>(Pair.class));
    desers.addDeserializer(Triple.class, new TupleDeserializer<>(Triple.class));
    desers.addDeserializer(Duet.class, new TupleDeserializer<>(Duet.class));
    desers.addDeserializer(Triplet.class, new TupleDeserializer<>(Triplet.class));
    desers.addDeserializer(Range.class, new TupleDeserializer<>(Range.class));
    desers.addDeserializer(Quartet.class, new TupleDeserializer<>(Quartet.class));
    desers.addDeserializer(Quintet.class, new TupleDeserializer<>(Quintet.class));
    desers.addDeserializer(Sextet.class, new TupleDeserializer<>(Sextet.class));
    desers.addDeserializer(Septet.class, new TupleDeserializer<>(Septet.class));
    desers.addDeserializer(Octet.class, new TupleDeserializer<>(Octet.class));
    desers.addDeserializer(Nonet.class, new TupleDeserializer<>(Nonet.class));
    desers.addDeserializer(Dectet.class, new TupleDeserializer<>(Dectet.class));
    context.addDeserializers(desers);

    SimpleSerializers sers = new SimpleSerializers();
    sers.addSerializer(Pair.class, new TupleSerializer<>(Pair.class));
    sers.addSerializer(Triple.class, new TupleSerializer<>(Triple.class));
    sers.addSerializer(Duet.class, new TupleSerializer<>(Duet.class));
    sers.addSerializer(Triplet.class, new TupleSerializer<>(Triplet.class));
    sers.addSerializer(Range.class, new TupleSerializer<>(Range.class));
    sers.addSerializer(Quartet.class, new TupleSerializer<>(Quartet.class));
    sers.addSerializer(Quintet.class, new TupleSerializer<>(Quintet.class));
    sers.addSerializer(Sextet.class, new TupleSerializer<>(Sextet.class));
    sers.addSerializer(Septet.class, new TupleSerializer<>(Septet.class));
    sers.addSerializer(Octet.class, new TupleSerializer<>(Octet.class));
    sers.addSerializer(Nonet.class, new TupleSerializer<>(Nonet.class));
    sers.addSerializer(Dectet.class, new TupleSerializer<>(Dectet.class));
    context.addSerializers(sers);
  }

  @Override
  public Version version() {
    return PackageVersion.VERSION;
  }

  /**
   * corant-modules-json
   *
   * @author bingo 15:06:03
   */
  public static class TupleDeserializer<T extends Tuple> extends JsonDeserializer<T> {

    protected final Class<T> clazz;

    public TupleDeserializer(Class<T> clazz) {
      this.clazz = clazz;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      if (clazz == Pair.class) {
        return (T) Pair.of(array[0], array[1]);
      } else if (clazz == Triple.class) {
        return (T) Triple.of(array[0], array[1], array[2]);
      } else if (clazz == Duet.class) {
        return (T) Tuple.duetOf(array[0], array[1]);
      } else if (clazz == Triplet.class) {
        return (T) Tuple.tripletOf(array[0], array[1], array[2]);
      } else if (clazz == Range.class) {
        return (T) Range.of((Comparable) array[0], (Comparable) array[1]);
      } else if (clazz == Quartet.class) {
        return (T) Tuple.quartetOf(array[0], array[1], array[2], array[3]);
      } else if (clazz == Quintet.class) {
        return (T) Tuple.quintetOf(array[0], array[1], array[2], array[3], array[4]);
      } else if (clazz == Sextet.class) {
        return (T) Tuple.sextetOf(array[0], array[1], array[2], array[3], array[4], array[5]);
      } else if (clazz == Septet.class) {
        return (T) Tuple.septetOf(array[0], array[1], array[2], array[3], array[4], array[5],
            array[6]);
      } else if (clazz == Octet.class) {
        return (T) Tuple.octetOf(array[0], array[1], array[2], array[3], array[4], array[5],
            array[6], array[7]);
      } else if (clazz == Nonet.class) {
        return (T) Tuple.nonetOf(array[0], array[1], array[2], array[3], array[4], array[5],
            array[6], array[7], array[8]);
      } else if (clazz == Dectet.class) {
        return (T) Tuple.dectetOf(array[0], array[1], array[2], array[3], array[4], array[5],
            array[6], array[7], array[8], array[9]);
      } else {
        throw new IOException("Can't deserialize class: " + clazz);
      }
    }

  }

  /**
   * corant-modules-json
   *
   * @author bingo 15:05:57
   */
  public static class TupleSerializer<T extends Tuple> extends JsonSerializer<T> {

    protected final int size;

    public TupleSerializer(Class<T> clazz) {
      if (clazz == Pair.class) {
        size = 2;
      } else if (clazz == Triple.class) {
        size = 3;
      } else if (clazz == Duet.class) {
        size = 2;
      } else if (clazz == Triplet.class) {
        size = 3;
      } else if (clazz == Range.class) {
        size = 2;
      } else if (clazz == Quartet.class) {
        size = 4;
      } else if (clazz == Quintet.class) {
        size = 5;
      } else if (clazz == Sextet.class) {
        size = 6;
      } else if (clazz == Septet.class) {
        size = 7;
      } else if (clazz == Octet.class) {
        size = 8;
      } else if (clazz == Nonet.class) {
        size = 9;
      } else if (clazz == Dectet.class) {
        size = 10;
      } else {
        throw new NotSupportedException("Can't support serialize class: " + clazz);
      }
    }

    @Override
    public void serialize(Tuple tuple, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(tuple, size);
      for (int i = 0; i < size; i++) {
        gen.writeObject(tuple.elementAt(i));
      }
      gen.writeEndArray();
    }
  }
}
