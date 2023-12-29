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

import java.util.HashMap;
import java.util.Map;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.corant.modules.bson.codec.BigDecimalDocumentCodec;
import org.corant.modules.bson.codec.BigIntegerCodec;
import org.corant.modules.bson.codec.ClassStringCodec;
import org.corant.modules.bson.codec.CurrencyStringCodec;
import org.corant.modules.bson.codec.DayOfWeekInt32Codec;
import org.corant.modules.bson.codec.DurationStringCodec;
import org.corant.modules.bson.codec.FullClassNameEnumCodec;
import org.corant.modules.bson.codec.LocalTimeStringCodec;
import org.corant.modules.bson.codec.LocaleStringCodec;
import org.corant.modules.bson.codec.MonthDayDocumentCodec;
import org.corant.modules.bson.codec.MonthDayStringCodec;
import org.corant.modules.bson.codec.MonthInt32Codec;
import org.corant.modules.bson.codec.OffsetDateTimeStringCodec;
import org.corant.modules.bson.codec.OffsetTimeStringCodec;
import org.corant.modules.bson.codec.PeriodDocumentCodec;
import org.corant.modules.bson.codec.PeriodStringCodec;
import org.corant.modules.bson.codec.SerializableCodec;
import org.corant.modules.bson.codec.URIStringCodec;
import org.corant.modules.bson.codec.URLStringCodec;
import org.corant.modules.bson.codec.YearInt32Codec;
import org.corant.modules.bson.codec.ZonedDateTimeStringCodec;
import org.corant.modules.bson.converter.BsonDatetimeSqlDateConverter;
import org.corant.modules.bson.converter.BsonTimestampTimestampConverter;

/**
 * corant-modules-json
 *
 * @author bingo 下午6:27:57
 */
public class ExtendedCodecProvider implements CodecProvider {

  private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();

  /**
   * A provider of Codecs for simple value types.
   */
  public ExtendedCodecProvider() {
    addCodecs();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
    if (clazz.isEnum()) {
      return (Codec<T>) FullClassNameEnumCodec.INSTANCE;
    }
    return (Codec<T>) codecs.get(clazz);
  }

  @Override
  public int hashCode() {
    return 0;
  }

  private <T> void addCodec(final Codec<T> codec) {
    codecs.put(codec.getEncoderClass(), codec);
  }

  private void addCodecs() {
    addCodec(new BigDecimalDocumentCodec());
    addCodec(new BigIntegerCodec());
    addCodec(new ClassStringCodec());
    addCodec(new CurrencyStringCodec());
    addCodec(new DayOfWeekInt32Codec());
    addCodec(new DurationStringCodec());
    addCodec(new LocaleStringCodec());
    addCodec(new LocalTimeStringCodec());
    addCodec(new MonthDayDocumentCodec());
    addCodec(new MonthDayStringCodec());
    addCodec(new MonthInt32Codec());
    addCodec(new OffsetDateTimeStringCodec());
    addCodec(new OffsetTimeStringCodec());
    addCodec(new PeriodDocumentCodec());
    addCodec(new PeriodStringCodec());
    addCodec(new SerializableCodec());
    addCodec(new BsonTimestampTimestampConverter());
    addCodec(new BsonDatetimeSqlDateConverter());
    addCodec(new URIStringCodec());
    addCodec(new URLStringCodec());
    addCodec(new YearInt32Codec());
    addCodec(new ZonedDateTimeStringCodec());
    addCodec(FullClassNameEnumCodec.INSTANCE);
  }

}
