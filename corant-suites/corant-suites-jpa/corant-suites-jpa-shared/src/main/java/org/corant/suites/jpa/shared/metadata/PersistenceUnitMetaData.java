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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.defaultString;
import javax.persistence.PersistenceUnit;
import org.corant.shared.normal.Names.PersistenceNames;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午11:03:42
 *
 */
public class PersistenceUnitMetaData {

  private final String name;
  private final String unitName;

  PersistenceUnitMetaData(final PersistenceUnit pu) {
    this(pu.name(), pu.unitName());
  }

  PersistenceUnitMetaData(final String name, final String unitName) {
    this.name = name;
    this.unitName = unitName;
  }

  public static PersistenceUnitMetaData of(final PersistenceUnit pu) {
    return new PersistenceUnitMetaData(pu);
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

  public String getMixedName() {
    String usePuName = defaultString(unitName, PersistenceNames.PU_DFLT_NME);
    usePuName = isEmpty(name) ? usePuName : usePuName + "." + name;
    return usePuName;
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
