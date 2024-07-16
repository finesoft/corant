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
package org.corant.modules.ddd;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Transient;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;
import org.corant.shared.util.Classes;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 上午11:58:12
 */
public final class DefaultAggregateIdentifier implements AggregateIdentifier {

  private static final long serialVersionUID = -930151000998600572L;

  private final Serializable id;

  private final String type;

  private volatile Class<? extends Aggregate> typeClass;

  @SuppressWarnings("unchecked")
  public DefaultAggregateIdentifier(Aggregate aggregate) {
    id = shouldNotNull(shouldNotNull(aggregate).getId());
    typeClass = (Class<? extends Aggregate>) Classes.getUserClass(aggregate);
    type = typeClass.getCanonicalName();
  }

  public DefaultAggregateIdentifier(Serializable id, Class<? extends Aggregate> typeClass) {
    this.id = shouldNotNull(id);
    this.typeClass = shouldNotNull(forceCast(Classes.getUserClass(typeClass)));
    type = this.typeClass.getCanonicalName();
  }

  DefaultAggregateIdentifier() {
    id = null;
    type = null;
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
    DefaultAggregateIdentifier other = (DefaultAggregateIdentifier) obj;
    return Objects.equals(id, other.id) && Objects.equals(type, other.type);
  }

  @Override
  public Serializable getId() {
    return id;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  @Transient
  @java.beans.Transient
  public Class<? extends Aggregate> getTypeClass() {
    if (typeClass == null) {
      synchronized (this) {
        typeClass = forceCast(Classes.tryAsClass(type));
      }
    }
    return typeClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type);
  }

  @Override
  public String toString() {
    return "{\"type\":\"" + type + "\",\"id\":" + id + "}";
  }
}
