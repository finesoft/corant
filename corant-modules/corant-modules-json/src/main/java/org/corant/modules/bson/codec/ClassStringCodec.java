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

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.corant.shared.util.Classes;

/**
 * corant-modules-json
 *
 * @author bingo 上午10:48:21
 *
 */
@SuppressWarnings("rawtypes")
public class ClassStringCodec implements Codec<Class> {

  @Override
  public Class decode(BsonReader reader, DecoderContext decoderContext) {
    return Classes.asClass(reader.readString());
  }

  @Override
  public void encode(BsonWriter writer, Class value, EncoderContext encoderContext) {
    writer.writeString(Classes.getUserClass(value).getCanonicalName());
  }

  @Override
  public Class<Class> getEncoderClass() {
    return Class.class;
  }
}
