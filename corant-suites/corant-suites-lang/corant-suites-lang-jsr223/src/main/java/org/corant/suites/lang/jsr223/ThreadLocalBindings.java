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
package org.corant.suites.lang.jsr223;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;

/**
 * corant-suites-lang-javascript
 *
 * @author bingo 下午2:36:37
 *
 */
public class ThreadLocalBindings implements Bindings {

  static ThreadLocal<Map<String, Object>> map = ThreadLocal.withInitial(HashMap::new);

  public ThreadLocalBindings() {
    if (map.get() == null) {
      map.set(new HashMap<>());
    }
  }

  @Override
  public void clear() {
    map.get().clear();
  }

  @Override
  public boolean containsKey(Object key) {
    checkKey(key);
    return map.get().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.get().containsValue(value);
  }

  public void destroy() {
    map.get().clear();
    map.remove();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.get().entrySet();
  }

  @Override
  public Object get(Object key) {
    checkKey(key);
    return map.get().get(key);
  }

  @Override
  public boolean isEmpty() {
    return map.get().isEmpty();
  }

  @Override
  public Set<String> keySet() {
    return map.get().keySet();
  }

  @Override
  public Object put(String name, Object value) {
    checkKey(name);
    return map.get().put(name, value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> toMerge) {
    if (toMerge == null) {
      throw new NullPointerException("toMerge map is null");
    }
    for (Map.Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
      String key = entry.getKey();
      checkKey(key);
      put(key, entry.getValue());
    }
  }

  @Override
  public Object remove(Object key) {
    checkKey(key);
    return map.get().remove(key);
  }

  @Override
  public int size() {
    return map.get().size();
  }

  @Override
  public Collection<Object> values() {
    return map.get().values();
  }

  void checkKey(Object key) {
    if (key == null) {
      throw new NullPointerException("key can not be null");
    }
    if (!(key instanceof String)) {
      throw new ClassCastException("key should be a String");
    }
    if (key.equals("")) {
      throw new IllegalArgumentException("key can not be empty");
    }
  }

}
