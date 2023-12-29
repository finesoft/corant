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

import java.time.Month;
import java.time.MonthDay;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * corant-modules-json
 *
 * @author bingo 上午11:14:43
 */
public class MonthDayDocumentCodec implements Codec<MonthDay> {

  private final String monthKey;

  private final String dayOfMonthKey;

  private final boolean monthAsString;

  public MonthDayDocumentCodec() {
    this("month", "dayOfMonth", false);
  }

  public MonthDayDocumentCodec(boolean monthAsString) {
    this("month", "dayOfMonth", monthAsString);
  }

  public MonthDayDocumentCodec(String monthKey, String dayOfMonthKey, boolean monthAsString) {
    this.monthKey = monthKey;
    this.dayOfMonthKey = dayOfMonthKey;
    this.monthAsString = monthAsString;
  }

  @Override
  public MonthDay decode(BsonReader reader, DecoderContext decoderContext) {
    MonthDay monthDay;
    reader.readStartDocument();

    if (monthAsString) {
      monthDay =
          MonthDay.of(Month.valueOf(reader.readString(monthKey)), reader.readInt32(dayOfMonthKey));
    } else {
      monthDay = MonthDay.of(reader.readInt32(monthKey), reader.readInt32(dayOfMonthKey));
    }

    reader.readEndDocument();

    return monthDay;
  }

  @Override
  public void encode(BsonWriter writer, MonthDay value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    if (monthAsString) {
      writer.writeString(monthKey, value.getMonth().name());
    } else {
      writer.writeInt32(monthKey, value.getMonthValue());
    }
    writer.writeInt32(dayOfMonthKey, value.getDayOfMonth());
    writer.writeEndDocument();
  }

  @Override
  public Class<MonthDay> getEncoderClass() {
    return MonthDay.class;
  }

}
