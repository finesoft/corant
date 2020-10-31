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
package org.corant.suites.json;

import java.time.temporal.Temporal;
import java.util.Map;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-suites-json
 *
 * @author bingo 上午12:06:55
 *
 */
@SuppressWarnings("rawtypes")
public class JacksonMapObjectConverterFactory implements ConverterFactory<Map, Object> {

  @Override
  public Converter<Map, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    return (t, h) -> JsonUtils.objectMapper.convertValue(t, targetClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return !Temporal.class.isAssignableFrom(targetClass);
  }

}
