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

import java.math.BigInteger;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

/**
 * corant-modules-json
 *
 * @author bingo 下午12:10:28
 *
 */
public class BigIntegerCodec implements Codec<BigInteger> {

  @Override
  public BigInteger decode(final BsonReader reader, final DecoderContext decoderContext) {
    return BigInteger.valueOf(reader.readDecimal128().longValue());
  }

  @Override
  public void encode(final BsonWriter writer, final BigInteger value,
      final EncoderContext encoderContext) {
    writer.writeDecimal128(new Decimal128(value.longValue()));
  }

  @Override
  public Class<BigInteger> getEncoderClass() {
    return BigInteger.class;
  }

}
