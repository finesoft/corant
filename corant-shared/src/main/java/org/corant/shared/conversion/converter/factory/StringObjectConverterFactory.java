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
package org.corant.shared.conversion.converter.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-shared
 *
 * Unfinish yet
 *
 * @author bingo 上午11:38:58
 *
 */
public class StringObjectConverterFactory implements ConverterFactory<String, Object> {

  @Override
  public Converter<String, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    return null;
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return false;
  }

  Constructor<?> findConstructor(Class<?> targetClass, Class<?>... argumentTypes) {
    try {
      Constructor<?> constructor = targetClass.getConstructor(argumentTypes);
      if (Modifier.isPublic(constructor.getModifiers())) {
        return constructor;
      } else {
        return null;
      }
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }

  Method findMethod(Class<?> targetClass, String method, Class<?>... argumentTypes) {
    try {
      Method factoryMethod = targetClass.getMethod(method, String.class);
      if (Modifier.isStatic(factoryMethod.getModifiers())
          && Modifier.isPublic(factoryMethod.getModifiers())) {
        return factoryMethod;
      } else {
        return null;
      }
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }
}