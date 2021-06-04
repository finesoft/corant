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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.ArrayList;
import java.util.List;
import org.corant.modules.jms.annotation.MessageProperty;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午5:15:27
 *
 */
public class MessagePropertyMetaData {
  private final String name;
  private final Class<?> type;
  private final String value;

  public MessagePropertyMetaData(String name, Class<?> type, String value) {
    this.name = name;
    this.type = type;
    this.value = MetaDataPropertyResolver.get(value, String.class);
  }

  public static MessagePropertyMetaData of(MessageProperty annotation) {
    shouldNotNull(annotation);
    return new MessagePropertyMetaData(annotation.name(), annotation.type(), annotation.value());
  }

  public static List<MessagePropertyMetaData> of(MessageProperty[] annotations) {
    shouldNotNull(annotations);
    List<MessagePropertyMetaData> metas = new ArrayList<>(annotations.length);
    for (MessageProperty a : annotations) {
      metas.add(of(a));
    }
    return metas;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

}
