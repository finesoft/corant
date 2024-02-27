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
package org.corant.modules.bson.codec;

import java.util.ArrayList;
import java.util.List;
import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.corant.shared.conversion.Conversion;

/**
 * corant-modules-json
 *
 * @author bingo 11:47:26
 */
public class PrimitiveArrayCodecs {

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class BooleanArrayCodec implements Codec<boolean[]> {

    @Override
    public boolean[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Boolean> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readBoolean());
      }
      reader.readEndArray();
      return Conversion.convert(temp, boolean[].class);
    }

    @Override
    public void encode(BsonWriter writer, boolean[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (boolean i : value) {
        writer.writeBoolean(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<boolean[]> getEncoderClass() {
      return boolean[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class ByteArrayCodec implements Codec<byte[]> {

    @Override
    public byte[] decode(BsonReader reader, DecoderContext decoderContext) {
      BsonBinary data = reader.readBinaryData();
      return data.getData();
    }

    @Override
    public void encode(BsonWriter writer, byte[] value, EncoderContext encoderContext) {
      writer.writeBinaryData(new BsonBinary(value));
    }

    @Override
    public Class<byte[]> getEncoderClass() {
      return byte[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class DoubleArrayCodec implements Codec<double[]> {

    @Override
    public double[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Double> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readDouble());
      }
      reader.readEndArray();
      return Conversion.convert(temp, double[].class);
    }

    @Override
    public void encode(BsonWriter writer, double[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (double i : value) {
        writer.writeDouble(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<double[]> getEncoderClass() {
      return double[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class FloatArrayCodec implements Codec<float[]> {

    @Override
    public float[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Double> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readDouble());
      }
      reader.readEndArray();
      return Conversion.convert(temp, float[].class);
    }

    @Override
    public void encode(BsonWriter writer, float[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (float i : value) {
        writer.writeDouble(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<float[]> getEncoderClass() {
      return float[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class IntArrayCodec implements Codec<int[]> {

    @Override
    public int[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Integer> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readInt32());
      }
      reader.readEndArray();
      return Conversion.convert(temp, int[].class);
    }

    @Override
    public void encode(BsonWriter writer, int[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (int i : value) {
        writer.writeInt32(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<int[]> getEncoderClass() {
      return int[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class LongArrayCodec implements Codec<long[]> {

    @Override
    public long[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Long> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readInt64());
      }
      reader.readEndArray();
      return Conversion.convert(temp, long[].class);
    }

    @Override
    public void encode(BsonWriter writer, long[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (long i : value) {
        writer.writeInt64(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<long[]> getEncoderClass() {
      return long[].class;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 12:07:49
   */
  public static class ShortArrayCodec implements Codec<short[]> {

    @Override
    public short[] decode(BsonReader reader, DecoderContext decoderContext) {
      List<Integer> temp = new ArrayList<>();
      reader.readStartArray();
      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        temp.add(reader.readInt32());
      }
      reader.readEndArray();
      return Conversion.convert(temp, short[].class);
    }

    @Override
    public void encode(BsonWriter writer, short[] value, EncoderContext encoderContext) {
      writer.writeStartArray();
      for (int i : value) {
        writer.writeInt32(i);
      }
      writer.writeEndArray();
    }

    @Override
    public Class<short[]> getEncoderClass() {
      return short[].class;
    }
  }
}
