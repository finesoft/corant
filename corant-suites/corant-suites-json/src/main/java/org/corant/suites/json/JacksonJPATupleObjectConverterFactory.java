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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-suites-json
 *
 * @author bingo 上午12:06:55
 *
 */
public class JacksonJPATupleObjectConverterFactory implements ConverterFactory<Tuple, Object> {

  @Override
  public Converter<Tuple, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    return (t, h) -> {
      List<TupleElement<?>> eles = t.getElements();
      Map<String, Object> tupleMap = new LinkedHashMap<>(eles.size());
      for (TupleElement<?> e : eles) {
        tupleMap.put(e.getAlias(), t.get(e));
      }
      if (Map.class.isAssignableFrom(targetClass)) {
        return tupleMap;
      }
      return JsonUtils.objectMapper.convertValue(tupleMap, targetClass);
    };
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return Tuple.class.equals(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return true;
  }

}
