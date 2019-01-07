/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.message;

import java.util.Map;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;

/**
 * @author bingo 下午3:52:48
 *
 */
public class BasePayloadBuilder {

  private final DynamicAttributeMap payload = new DynamicAttributeMap();

  private BasePayloadBuilder() {}

  public static BasePayloadBuilder instance() {
    return new BasePayloadBuilder();
  }

  public DynamicAttributeMap build() {
    return payload;
  }

  public BasePayloadBuilder set(Map<String, Object> cmd) {
    if (cmd != null) {
      payload.putAll(cmd);
    }
    return this;
  }

  public BasePayloadBuilder set(String key, Object value) {
    payload.put(key, value);
    return this;
  }
}
