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
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午5:04:39
 *
 */
public interface MongoClientConfigurator extends Sortable {

  static Map<String, Pair<Method, Class<?>[]>> createSettingsMap() {
    Map<String, Pair<Method, Class<?>[]>> settingsMap = new HashMap<>();
    Method[] methods = MongoClientSettings.Builder.class.getDeclaredMethods();
    for (Method method : methods) {
      if (method.getParameterCount() == 1) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (String.class.equals(parameterTypes[0]) || int.class.equals(parameterTypes[0])
            || boolean.class.equals(parameterTypes[0])) {
          settingsMap.put(method.getName(), Pair.of(method, parameterTypes));
        }
      }
    }
    return settingsMap;
  }

  void configure(MongoClientConfig config, Builder optionsBuilder);
}
