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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.forceCast;
import org.bson.BSONException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.corant.shared.util.Classes;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:43:07
 */
public class FullClassNameEnumCodec implements Codec<Enum<?>> {

  public static final FullClassNameEnumCodec INSTANCE = new FullClassNameEnumCodec();

  @Override
  public Enum<?> decode(BsonReader reader, DecoderContext decoderContext) {
    String value = reader.readString();
    String name = value.substring(value.lastIndexOf('.') + 1);
    Class<?> cls = Classes.asClass(value.substring(0, value.lastIndexOf('.')));
    shouldBeTrue(cls.isEnum());
    for (Object each : cls.getEnumConstants()) {
      if (((Enum<?>) each).name().equalsIgnoreCase(name)) {
        return (Enum<?>) each;
      }
    }
    throw new BSONException("Can't decode enum");
  }

  @Override
  public void encode(BsonWriter writer, Enum<?> value, EncoderContext encoderContext) {
    writer.writeString(value.getClass().getCanonicalName().concat(".").concat(value.name()));
  }

  @Override
  public Class<Enum<?>> getEncoderClass() {
    return forceCast(Enum.class);
  }
}
