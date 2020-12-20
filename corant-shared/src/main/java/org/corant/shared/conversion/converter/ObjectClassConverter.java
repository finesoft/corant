/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.conversion.converter;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午11:48:25
 *
 */
@SuppressWarnings("rawtypes")
public class ObjectClassConverter extends AbstractConverter<Object, Class> {

  public ObjectClassConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public ObjectClassConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public ObjectClassConverter(Class defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public ObjectClassConverter(Class defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Class convert(Object value, Map<String, ?> hints) throws Exception {
    if (value == null) {
      return getDefaultValue();
    }
    if (value instanceof Class) {
      return (Class) value;
    } else {
      ClassLoader cl = defaultObject(
          ConverterHints.getHint(hints, ConverterHints.CVT_CLS_LOADER_KEY), defaultClassLoader());
      return cl.loadClass(value.toString());
    }
  }

}