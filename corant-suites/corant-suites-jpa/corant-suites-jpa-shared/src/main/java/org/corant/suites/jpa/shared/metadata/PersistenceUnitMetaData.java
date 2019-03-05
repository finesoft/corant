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

import static org.corant.shared.util.StringUtils.defaultTrim;
import javax.persistence.PersistenceUnit;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午6:05:59
 *
 */
public class PersistenceUnitMetaData {

  private final String name;
  private final String unitName;

  public PersistenceUnitMetaData(PersistenceUnit pu) {
    name = pu.name();
    unitName = defaultTrim(pu.unitName());
  }

  /**
   * @param name
   * @param unitName
   */
  public PersistenceUnitMetaData(String name, String unitName) {
    super();
    this.name = name;
    this.unitName = unitName;
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
    PersistenceUnitMetaData other = (PersistenceUnitMetaData) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
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

  public String getName() {
    return name;
  }

  public String getUnitName() {
    return unitName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    result = prime * result + (unitName == null ? 0 : unitName.hashCode());
    return result;
  }

}
