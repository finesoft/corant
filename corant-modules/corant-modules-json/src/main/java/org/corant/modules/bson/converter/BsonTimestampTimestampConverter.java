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
package org.corant.modules.bson.converter;

import java.sql.Timestamp;
import java.util.Map;
import org.bson.BsonReader;
import org.bson.BsonTimestamp;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.corant.shared.conversion.converter.AbstractConverter;

/**
 * corant-modules-json
 *
 * @author bingo 上午10:14:32
 */
public class BsonTimestampTimestampConverter extends AbstractConverter<BsonTimestamp, Timestamp>
    implements Codec<Timestamp> {

  @Override
  public Timestamp decode(BsonReader reader, DecoderContext decoderContext) {
    return new Timestamp(reader.readDateTime());
  }

  @Override
  public void encode(BsonWriter writer, Timestamp value, EncoderContext encoderContext) {
    writer.writeDateTime(value.getTime());
  }

  @Override
  public Class<Timestamp> getEncoderClass() {
    return Timestamp.class;
  }

  @Override
  protected Timestamp doConvert(BsonTimestamp value, Map<String, ?> hints) throws Exception {
    return value == null ? null : new Timestamp(value.getValue());
  }

}
