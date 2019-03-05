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
package org.corant.suites.jpa.shared.metadata;

import static org.corant.shared.util.Assertions.shouldBeEquals;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.defaultTrim;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:49:31
 *
 */
public class PersistenceContextMetaData {

  private final PersistenceContextType type;
  private final SynchronizationType synchronization;
  private final Map<String, String> properties;
  private final String unitName;
  private PersistenceUnitInfoMetaData unit;

  public PersistenceContextMetaData(PersistenceContext pc) {
    shouldBeTrue(pc.synchronization() == SynchronizationType.SYNCHRONIZED,
        "Only support SYNCHRONIZED persistence context!");
    type = pc.type();
    synchronization = pc.synchronization();
    unitName = defaultTrim(pc.unitName());
    Map<String, String> map = new HashMap<>();
    if (!isEmpty(pc.properties())) {
      for (PersistenceProperty pp : pc.properties()) {
        map.put(pp.name(), pp.value());
      }
    }
    properties = Collections.unmodifiableMap(map);
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
    PersistenceContextMetaData other = (PersistenceContextMetaData) obj;
    if (properties == null) {
      if (other.properties != null) {
        return false;
      }
    } else if (!properties.equals(other.properties)) {
      return false;
    }
    if (synchronization != other.synchronization) {
      return false;
    }
    if (type != other.type) {
      return false;
    }
    if (unitName == null) {
      if (other.unitName != null) {
        return false;
      }
    } else if (!unitName.equals(other.unitName)) {
      return false;
    }
    return true;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public SynchronizationType getSynchronization() {
    return synchronization;
  }

  public PersistenceContextType getType() {
    return type;
  }

  public PersistenceUnitInfoMetaData getUnit() {
    return unit;
  }

  public String getUnitName() {
    return unitName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (properties == null ? 0 : properties.hashCode());
    result = prime * result + (synchronization == null ? 0 : synchronization.hashCode());
    result = prime * result + (type == null ? 0 : type.hashCode());
    result = prime * result + (unitName == null ? 0 : unitName.hashCode());
    return result;
  }

  public void setUnit(PersistenceUnitInfoMetaData unit) {
    shouldBeEquals(shouldNotNull(unit).getPersistenceUnitName(), unitName);
    this.unit = unit;
  }

}
