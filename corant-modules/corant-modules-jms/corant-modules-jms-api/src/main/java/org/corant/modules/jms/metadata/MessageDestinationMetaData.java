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

import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.get;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getBoolean;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.newHashMap;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.modules.jms.annotation.MessageDestination;
import org.corant.modules.jms.annotation.MessageProperty;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:08:29
 *
 */
public class MessageDestinationMetaData {

  private final String connectionFactoryId;

  private final String name;

  private final boolean multicast;

  private final Map<String, Object> properties;

  public MessageDestinationMetaData(String connectionFactoryId, String name, boolean multicast,
      Map<String, Object> properties) {
    this.connectionFactoryId = MetaDataPropertyResolver.get(connectionFactoryId, String.class);
    this.name = MetaDataPropertyResolver.get(name, String.class);
    this.multicast = multicast;
    this.properties = Collections.unmodifiableMap(newHashMap(properties));
  }

  public static Set<MessageDestinationMetaData> from(AnnotatedElement clazz) {
    shouldNotNull(clazz);
    Set<MessageDestinationMetaData> metas = new LinkedHashSet<>();
    listOf(clazz.getAnnotationsByType(MessageDestination.class)).stream()
        .map(MessageDestinationMetaData::of).forEach(metas::add);
    return metas;
  }

  public static MessageDestinationMetaData of(MessageDestination annotation) {
    shouldNotNull(annotation);
    Map<String, Object> properties = new HashMap<>();
    for (MessageProperty mp : annotation.properties()) {
      properties.put(mp.name(), get(mp.value(), mp.type()));
    }
    return new MessageDestinationMetaData(annotation.connectionFactoryId(), annotation.name(),
        getBoolean(annotation.multicast()), properties);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MessageDestinationMetaData other = (MessageDestinationMetaData) obj;
    if (connectionFactoryId == null) {
      if (other.connectionFactoryId != null) {
        return false;
      }
    } else if (!connectionFactoryId.equals(other.connectionFactoryId)) {
      return false;
    }
    if (multicast != other.multicast) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  public String getName() {
    return name;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
    result = prime * result + (multicast ? 1231 : 1237);
    return prime * result + (name == null ? 0 : name.hashCode());
  }

  public boolean isMulticast() {
    return multicast;
  }

}
