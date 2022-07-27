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

import java.time.Duration;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * corant-modules-json
 *
 * @author bingo 上午11:13:48
 *
 */
public class DurationStringCodec implements Codec<Duration> {

  @Override
  public Duration decode(BsonReader reader, DecoderContext decoderContext) {
    return Duration.parse(reader.readString());
  }

  @Override
  public void encode(BsonWriter writer, Duration value, EncoderContext encoderContext) {
    writer.writeString(value.toString());
  }

  @Override
  public Class<Duration> getEncoderClass() {
    return Duration.class;
  }

}
