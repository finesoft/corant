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

import java.sql.Date;
import java.util.Map;
import org.bson.BsonDateTime;
import org.bson.BsonReader;
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
public class BsonDatetimeSqlDateConverter extends AbstractConverter<BsonDateTime, Date>
    implements Codec<Date> {

  @Override
  public Date decode(BsonReader reader, DecoderContext decoderContext) {
    return new Date(reader.readDateTime());
  }

  @Override
  public void encode(BsonWriter writer, Date value, EncoderContext encoderContext) {
    writer.writeDateTime(value.getTime());
  }

  @Override
  public Class<Date> getEncoderClass() {
    return Date.class;
  }

  @Override
  protected Date doConvert(BsonDateTime value, Map<String, ?> hints) throws Exception {
    return value == null ? null : new Date(value.getValue());
  }

}
