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
import java.beans.Transient;
import java.io.Serializable;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;
import org.corant.shared.util.Objects;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 上午11:58:12
 */
public final class DefaultAggregateIdentifier implements AggregateIdentifier {

  private static final long serialVersionUID = -930151000998600572L;

  private final Serializable id;

  private final Class<? extends Aggregate> typeCls;

  private final int hash;

  public DefaultAggregateIdentifier(Aggregate aggregate) {
    id = shouldNotNull(shouldNotNull(aggregate).getId());
    typeCls = aggregate.getClass();
    hash = Objects.hash(id, typeCls);
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
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (typeCls == null) {
      return other.typeCls == null;
    } else {
      return typeCls.equals(other.typeCls);
    }
  }

  @Override
  public Serializable getId() {
    return id;
  }

  @Override
  public String getType() {
    return typeCls == null ? null : typeCls.getName();
  }

  @Override
  @Transient
  public Class<? extends Aggregate> getTypeCls() {
    return typeCls;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "{\"typeCls\":\"" + typeCls + "\",\"id\":" + id + "}";
  }

}
