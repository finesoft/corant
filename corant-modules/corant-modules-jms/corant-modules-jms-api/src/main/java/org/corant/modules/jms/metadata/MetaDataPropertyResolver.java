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
package org.corant.modules.jms.metadata;

import static org.corant.shared.util.Conversions.toObject;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Systems;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:35:09
 *
 */
public interface MetaDataPropertyResolver extends Sortable {

  String VAR_PREFIX = "${";
  String VAR_SUFFIX = "}";

  static <T> T get(String property, Class<T> clazz) {
    if (property == null) {
      return null;
    }
    MetaDataPropertyResolver resolver = null;
    try {
      Instance<MetaDataPropertyResolver> inst =
          CDI.current().select(MetaDataPropertyResolver.class);
      if (!inst.isUnsatisfied()) {
        resolver = inst.stream().sorted(Sortable::compare).findFirst().get();
      }
    } catch (Exception ex) {
      // ignore
    }
    if (resolver != null) {
      return resolver.resolve(property, clazz);
    }
    if (property.contains(VAR_PREFIX) && property.contains(VAR_SUFFIX)) {
      return toObject(resolveVariable(property), clazz);
    }
    return toObject(property, clazz);
  }

  static boolean getBoolean(String property) {
    return get(property, Boolean.class);
  }

  static double getDouble(String property) {
    return get(property, Double.class);
  }

  static double getFloat(String property) {
    return get(property, Float.class);
  }

  static int getInt(String property) {
    return get(property, Integer.class);
  }

  static long getLong(String property) {
    return get(property, Long.class);
  }

  static String getString(String property) {
    return get(property, String.class);
  }

  static String resolveVariable(String propertyName) {
    int startVar = 0;
    String resolvedValue = propertyName;
    while ((startVar = resolvedValue.indexOf(VAR_PREFIX, startVar)) >= 0) {
      int endVar = resolvedValue.indexOf(VAR_SUFFIX, startVar);
      if (endVar <= 0) {
        break;
      }
      String varName = resolvedValue.substring(startVar + 2, endVar);
      if (varName.isEmpty()) {
        break;
      }
      String varVal = Systems.getProperty(varName);
      if (varVal != null) {
        resolvedValue =
            resolveVariable(resolvedValue.replace(VAR_PREFIX + varName + VAR_SUFFIX, varVal));
      }
      startVar++;
    }
    return resolvedValue;
  }

  <T> T resolve(String property, Class<T> clazz);
}
