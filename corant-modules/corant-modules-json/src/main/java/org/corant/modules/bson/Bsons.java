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
package org.corant.modules.bson;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Map;
import org.bson.BsonBinary;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinarySubType;
import org.bson.BsonBinaryWriter;
import org.bson.BsonBoolean;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.ByteBufNIO;
import org.bson.Document;
import org.bson.Transformer;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:41:42
 *
 */
public class Bsons {

  public static final BsonTypeClassMap DEFAULT_BSON_TYPE_CLASS_MAP = new BsonTypeClassMap();

  public static final Transformer SIMPLE_TYPE_DECODE_TRANSFORMER = Bsons::decodeTransformer;

  public static final DocumentCodecProvider DOCUMENT_CODEC_PROVIDER =
      new DocumentCodecProvider(DEFAULT_BSON_TYPE_CLASS_MAP, SIMPLE_TYPE_DECODE_TRANSFORMER);

  public static final CodecRegistry DOCUMENT_CODEC_REGISTRY =
      fromProviders(asList(new ValueCodecProvider(), new BsonValueCodecProvider(),
          new Jsr310CodecProvider(), DOCUMENT_CODEC_PROVIDER, new ExtendedCodecProvider()));

  public static final Codec<Document> DEFAULT_DOCUMENT_CODEC = new DocumentCodec(
      DOCUMENT_CODEC_REGISTRY, DEFAULT_BSON_TYPE_CLASS_MAP, SIMPLE_TYPE_DECODE_TRANSFORMER);

  public static final CodecRegistry BSON_DOCUMENT_REGISTRY =
      fromProviders(new BsonValueCodecProvider(), new ExtendedCodecProvider());

  public static final DecoderContext DEFAULT_DECODER_CONTEXT = DecoderContext.builder().build();
  public static final EncoderContext DEFAULT_ENCODER_CONTEXT =
      EncoderContext.builder().isEncodingCollectibleDocument(true).build();

  /**
   * Returns a map for the given bson bytes array, the mapped value type is BsonType.
   *
   * @param bsonBytes the given bson types array
   * @return a map for the given bson bytes array, the mapped value type is BsonType.
   */
  public static Map<String, Object> fromBytes(byte[] bsonBytes) {
    return fromBytes(bsonBytes, DEFAULT_DOCUMENT_CODEC, DEFAULT_DECODER_CONTEXT);
  }

  /**
   * Returns a map for the given bson bytes array, the mapped value type is BsonType.
   *
   * @param bsonBytes the given bson types array
   * @param codec the Codec for decoding
   * @param decodeContext the context for decoding
   * @return a map for the given bson bytes array, the mapped value type is BsonType.
   */
  public static Map<String, Object> fromBytes(byte[] bsonBytes, Codec<Document> codec,
      DecoderContext decodeContext) {
    if (bsonBytes == null || bsonBytes.length == 0) {
      return emptyMap();
    }
    try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bsonBytes))) {
      return codec.decode(reader, decodeContext);
    }
  }

  /**
   * Converts the given bson value to java type object
   *
   * @param value the given bson value
   * @return a java type object
   */
  public static Object fromSimpleBsonValue(BsonValue value) {
    switch (shouldNotNull(value).getBsonType()) {
      case INT32:
        return value.asInt32().getValue();
      case INT64:
        return value.asInt64().getValue();
      case STRING:
        return value.asString().getValue();
      case DECIMAL128:
        return value.asDecimal128().decimal128Value().bigDecimalValue();
      case DOUBLE:
        return value.asDouble().getValue();
      case BOOLEAN:
        return value.asBoolean().getValue();
      case OBJECT_ID:
        return value.asObjectId().getValue();
      case BINARY:
        return value.asBinary().getData();
      case DATE_TIME:
        return new Date(value.asDateTime().getValue());
      case SYMBOL:
        return value.asSymbol().getSymbol();
      case ARRAY:
        return value.asArray().toArray();
      default:
        return value;
    }
  }

  /**
   * Returns a BsonDocument object for the given bson bytes array.
   *
   * @param bsonBytes the given bson types array
   * @return a BsonDocument object for the given bson bytes array.
   */
  public static BsonDocument toBsonDocument(byte[] bsonBytes) {
    if (bsonBytes == null || bsonBytes.length == 0) {
      return new BsonDocument();
    }
    try (
        ByteBufferBsonInput input = new ByteBufferBsonInput(new ByteBufNIO(
            ByteBuffer.wrap(bsonBytes, 0, bsonBytes.length).order(ByteOrder.LITTLE_ENDIAN)));
        BsonBinaryReader bsonReader = new BsonBinaryReader(input);) {
      return new BsonDocumentCodec(BSON_DOCUMENT_REGISTRY).decode(bsonReader,
          DecoderContext.builder().build());
    }
  }

  /**
   * Convert given bson document to bytes array
   *
   * @param document the document to be converted
   * @return a bytes array
   */
  public static byte[] toBytes(BsonDocument document) {
    if (document == null) {
      return Bytes.EMPTY_ARRAY;
    }
    return document.asBinary().getData();
  }

  /**
   * Convert given maps object to bson bytes array
   *
   * @param bsonObject the maps object
   * @return a bson bytes array
   */
  public static byte[] toBytes(Map<String, Object> bsonObject) {
    return toBytes(bsonObject, DEFAULT_DOCUMENT_CODEC, DEFAULT_ENCODER_CONTEXT);
  }

  /**
   * Convert given maps object to bson bytes array
   *
   * @param bsonObject the maps object
   * @param codec the Codec for encoding
   * @param encoderContext the context for encoding
   * @return a bson bytes array
   */
  public static byte[] toBytes(Map<String, Object> bsonObject, Codec<Document> codec,
      EncoderContext encoderContext) {
    if (bsonObject == null) {
      return Bytes.EMPTY_ARRAY;
    }
    try (BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);) {
      Document doc =
          bsonObject instanceof Document ? (Document) bsonObject : new Document(bsonObject);
      codec.encode(writer, doc, encoderContext);
      return buffer.toByteArray();
    }
  }

  /**
   * Returns a bson value that is converted from the given simple type object.
   *
   * @param source the given simple type object, support types:
   *        ObjectId/String/Double/Integer/Float/Boolean/byte[]/Long/Resource/BigDecimal/BigInteger
   * @return a bson value
   */
  public static BsonValue toSimpleBsonValue(Object source) {
    if (source == null) {
      return null;
    }
    if (source instanceof BsonValue) {
      return (BsonValue) source;
    }
    if (source instanceof ObjectId) {
      return new BsonObjectId((ObjectId) source);
    }
    if (source instanceof String) {
      return new BsonString((String) source);
    }
    if (source instanceof Double) {
      return new BsonDouble((Double) source);
    }
    if (source instanceof Integer) {
      return new BsonInt32((Integer) source);
    }
    if (source instanceof Long) {
      return new BsonInt64((Long) source);
    }
    if (source instanceof byte[]) {
      return new BsonBinary((byte[]) source);
    }
    if (source instanceof Boolean) {
      return new BsonBoolean((Boolean) source);
    }
    if (source instanceof Float) {
      return new BsonDouble((Float) source);
    }
    if (source instanceof BigInteger) {
      return new BsonDecimal128(new Decimal128(new BigDecimal((BigInteger) source)));
    }
    if (source instanceof BigDecimal) {
      return new BsonDecimal128(new Decimal128((BigDecimal) source));
    }
    if (source instanceof Resource) {
      try {
        return new BsonBinary(((Resource) source).getBytes());
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    throw new IllegalArgumentException(String.format("Unable to convert %s (%s) to BsonValue.",
        source, source.getClass().getName()));
  }

  static Object decodeTransformer(Object objectToTransform) {
    if (objectToTransform instanceof Binary) {
      Binary binary = (Binary) objectToTransform;
      if (binary.getType() == BsonBinarySubType.BINARY.getValue()) {
        return binary.getData();
      }
    }
    return objectToTransform;
  }
}
