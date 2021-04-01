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
package org.corant.modules.mongodb;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午5:04:39
 *
 */
public interface MongoClientConfigurator {

  static Map<String, Method> createSettingsMap() {
    Map<String, Method> settingsMap = new HashMap<>();
    Method[] methods = MongoClientOptions.Builder.class.getDeclaredMethods();
    for (Method method : methods) {
      if (method.getParameterTypes().length == 1) {
        Class<?> parameterType = method.getParameterTypes()[0];
        if (String.class.equals(parameterType) || int.class.equals(parameterType)
            || boolean.class.equals(parameterType)) {
          settingsMap.put(method.getName(), method);
        }
      }
    }
    return settingsMap;
  }

  void configure(Builder optionsBuilder);
}
