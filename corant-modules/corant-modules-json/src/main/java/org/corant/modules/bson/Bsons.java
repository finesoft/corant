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
import static org.corant.shared.util.Lists.listOf;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.AbstractBsonWriter;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinarySubType;
import org.bson.BsonBinaryWriter;
import org.bson.BsonBoolean;
import org.bson.BsonContextType;
import org.bson.BsonDateTime;
import org.bson.BsonDbPointer;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonJavaScript;
import org.bson.BsonNull;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonString;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.BsonUndefined;
import org.bson.BsonValue;
import org.bson.BsonWriterSettings;
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
import org.bson.codecs.configuration.CodecConfigurationException;
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
import org.corant.shared.util.Primitives;

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
        BsonBinaryReader bsonReader = new BsonBinaryReader(input)) {
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
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
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
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static BsonValue toSimpleBsonValue(Object source) {
    if (source == null) {
      return BsonNull.VALUE;
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
    if (source instanceof Short) {
      return new BsonInt32((Short) source);
    }
    if (source instanceof Byte) {
      return new BsonInt32((Byte) source);
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
    if (source instanceof Date) {
      return new BsonDateTime(((Date) source).getTime());
    }
    if (source instanceof Instant) {
      return new BsonDateTime(((Instant) source).toEpochMilli());
    }
    if (source instanceof ZonedDateTime) {
      return new BsonDateTime(((ZonedDateTime) source).toInstant().toEpochMilli());
    }
    if (source instanceof Timestamp) {
      Timestamp ts = (Timestamp) source;
      return new BsonTimestamp(ts.toInstant().getEpochSecond());
    }
    if (source instanceof Resource) {
      try {
        return new BsonBinary(((Resource) source).getBytes());
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    try {
      Object value = source;
      if (Primitives.isPrimitiveArray(source.getClass())) {
        value = listOf(Primitives.wrapArray(source));
      }
      Codec codec = BSON_DOCUMENT_REGISTRY.get(value.getClass());
      SimpleBsonWriter writer = new SimpleBsonWriter(value.getClass());
      codec.encode(writer, value,
          value instanceof Collection<?> || value instanceof Object[]
              ? EncoderContext.builder().build()
              : null);
      return writer.getValue();
    } catch (CodecConfigurationException e) {
      throw new IllegalArgumentException(String.format("Unable to convert %s to BsonValue.",
          source != null ? source.getClass().getName() : "null"));
    }
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

  /**
   * corant-modules-json
   *
   * @author bingo 下午5:30:28
   *
   */
  static class SimpleBsonWriter extends AbstractBsonWriter {

    private final List<BsonValue> values = new ArrayList<>();

    public SimpleBsonWriter(Class<?> type) {
      super(new BsonWriterSettings());

      if (Map.class.isAssignableFrom(type)) {
        setContext(new Context(null, BsonContextType.DOCUMENT));
      } else if (Collection.class.isAssignableFrom(type) || type.isArray()) {
        setContext(new Context(null, BsonContextType.ARRAY));
      } else {
        setContext(new Context(null, BsonContextType.DOCUMENT));
      }
    }

    @Override
    public void flush() {
      values.clear();
    }

    @Override
    public void writeEndArray() {
      setState(State.NAME);
    }

    @Override
    public void writeStartArray() {
      setState(State.VALUE);
    }

    @Override
    protected void doWriteBinaryData(BsonBinary value) {
      values.add(value);
    }

    @Override
    protected void doWriteBoolean(boolean value) {
      values.add(BsonBoolean.valueOf(value));
    }

    @Override
    protected void doWriteDateTime(long value) {
      values.add(new BsonDateTime(value));
    }

    @Override
    protected void doWriteDBPointer(BsonDbPointer value) {
      values.add(value);
    }

    @Override
    protected void doWriteDecimal128(Decimal128 value) {
      values.add(new BsonDecimal128(value));
    }

    @Override
    protected void doWriteDouble(double value) {
      values.add(new BsonDouble(value));
    }

    @Override
    protected void doWriteEndArray() {}

    @Override
    protected void doWriteEndDocument() {}

    @Override
    protected void doWriteInt32(int value) {
      values.add(new BsonInt32(value));
    }

    @Override
    protected void doWriteInt64(long value) {
      values.add(new BsonInt64(value));
    }

    @Override
    protected void doWriteJavaScript(String value) {
      values.add(new BsonJavaScript(value));
    }

    @Override
    protected void doWriteJavaScriptWithScope(String value) {
      throw new UnsupportedOperationException("Cannot capture JavaScriptWith");
    }

    @Override
    protected void doWriteMaxKey() {}

    @Override
    protected void doWriteMinKey() {}

    @Override
    protected void doWriteNull() {
      values.add(new BsonNull());
    }

    @Override
    protected void doWriteObjectId(ObjectId value) {
      values.add(new BsonObjectId(value));
    }

    @Override
    protected void doWriteRegularExpression(BsonRegularExpression value) {
      values.add(value);
    }

    @Override
    protected void doWriteStartArray() {}

    @Override
    protected void doWriteStartDocument() {}

    @Override
    protected void doWriteString(String value) {
      values.add(new BsonString(value));
    }

    @Override
    protected void doWriteSymbol(String value) {
      values.add(new BsonSymbol(value));
    }

    @Override
    protected void doWriteTimestamp(BsonTimestamp value) {
      values.add(value);
    }

    @Override
    protected void doWriteUndefined() {
      values.add(new BsonUndefined());
    }

    BsonValue getValue() {
      if (values.isEmpty()) {
        return null;
      }
      if (!BsonContextType.ARRAY.equals(getContext().getContextType())) {
        return values.get(0);
      }
      return new BsonArray(values);
    }
  }
}
