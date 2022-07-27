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

import java.time.Period;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * corant-modules-json
 *
 * @author bingo 上午11:15:04
 *
 */
public class PeriodDocumentCodec implements Codec<Period> {

  private final String yearsKey;

  private final String monthsKey;

  private final String daysKey;

  public PeriodDocumentCodec() {
    yearsKey = "years";
    monthsKey = "months";
    daysKey = "days";
  }

  public PeriodDocumentCodec(String yearsKey, String monthsKey, String daysKey) {
    this.yearsKey = yearsKey;
    this.monthsKey = monthsKey;
    this.daysKey = daysKey;
  }

  @Override
  public Period decode(BsonReader reader, DecoderContext decoderContext) {
    reader.readStartDocument();
    Period period = Period.of(reader.readInt32(yearsKey), reader.readInt32(monthsKey),
        reader.readInt32(daysKey));
    reader.readEndDocument();
    return period;
  }

  @Override
  public void encode(BsonWriter writer, Period value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    writer.writeInt32(yearsKey, value.getYears());
    writer.writeInt32(monthsKey, value.getMonths());
    writer.writeInt32(daysKey, value.getDays());
    writer.writeEndDocument();
  }

  @Override
  public Class<Period> getEncoderClass() {
    return Period.class;
  }

}
