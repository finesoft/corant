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
package org.corant.modules.bson.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bson.BsonBinary;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * corant-modules-json
 *
 * @author bingo 上午11:28:02
 */
public class SerializableCodec implements Codec<Serializable> {
  private final boolean compress;

  public SerializableCodec() {
    this(false);
  }

  public SerializableCodec(boolean compress) {
    this.compress = compress;
  }

  @Override
  public Serializable decode(BsonReader reader, DecoderContext decoderContext) {
    BsonBinary bsonBinary = reader.readBinaryData();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bsonBinary.getData());
        InputStream is = compress ? new GZIPInputStream(bais) : bais) {
      final ObjectInputStream ois = new ObjectInputStream(is);
      return (Serializable) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new BsonInvalidOperationException(e.getMessage());
    }
  }

  @Override
  public void encode(BsonWriter writer, Serializable value, EncoderContext encoderContext) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = compress ? new GZIPOutputStream(baos) : baos;
        ObjectOutputStream oos = new ObjectOutputStream(os)) {
      oos.writeObject(value);
      oos.flush();
      writer.writeBinaryData(new BsonBinary(baos.toByteArray()));
    } catch (IOException e) {
      throw new BsonInvalidOperationException(e.getMessage());
    }
  }

  @Override
  public Class<Serializable> getEncoderClass() {
    return Serializable.class;
  }
}
