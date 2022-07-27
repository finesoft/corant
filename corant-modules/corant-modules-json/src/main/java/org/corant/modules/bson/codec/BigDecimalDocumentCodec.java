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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * corant-modules-json
 *
 * @author bingo 上午10:51:39
 *
 */
public class BigDecimalDocumentCodec implements Codec<BigDecimal> {

  private final String unscaledKey;
  private final String scaleKey;
  private final int scaleThreshold;

  public BigDecimalDocumentCodec() {
    this("unscaled", "scale", 18);
  }

  public BigDecimalDocumentCodec(String unscaledKey, String scaleKey) {
    this(unscaledKey, scaleKey, 18);
  }

  public BigDecimalDocumentCodec(String unscaledKey, String scaleKey, int scaleThreshold) {
    this.unscaledKey = unscaledKey;
    this.scaleKey = scaleKey;
    this.scaleThreshold = scaleThreshold;
  }

  @Override
  public BigDecimal decode(BsonReader reader, DecoderContext decoderContext) {
    reader.readStartDocument();
    BigDecimal value = new BigDecimal(BigInteger.valueOf(reader.readInt64(unscaledKey)),
        reader.readInt32(scaleKey));
    reader.readEndDocument();
    return value;
  }

  @Override
  public void encode(BsonWriter writer, BigDecimal value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    BigDecimal val = value;
    if (val.scale() > scaleThreshold) {
      val = value.setScale(scaleThreshold, RoundingMode.HALF_UP);
    }
    writer.writeInt64(unscaledKey, val.unscaledValue().longValue());
    writer.writeInt32(scaleKey, val.scale());
    writer.writeEndDocument();
  }

  @Override
  public Class<BigDecimal> getEncoderClass() {
    return BigDecimal.class;
  }
}
