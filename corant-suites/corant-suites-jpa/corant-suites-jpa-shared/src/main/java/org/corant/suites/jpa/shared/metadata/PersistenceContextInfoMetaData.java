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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
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
public class PersistenceContextInfoMetaData {

  private final PersistenceContextType type;
  private final SynchronizationType synchronization;
  private final Map<String, String> properties;
  private final PersistenceUnitInfoMetaData unit;

  private PersistenceContextInfoMetaData(PersistenceContext pc, PersistenceUnitInfoMetaData unit) {
    shouldBeTrue(pc.synchronization() == SynchronizationType.SYNCHRONIZED,
        "Only support SYNCHRONIZED persistence context!");
    type = pc.type();
    synchronization = pc.synchronization();
    this.unit = unit;
    Map<String, String> map = new HashMap<>();
    if (!isEmpty(pc.properties())) {
      for (PersistenceProperty pp : pc.properties()) {
        map.put(pp.name(), pp.value());
      }
    }
    properties = Collections.unmodifiableMap(map);
  }

  public static PersistenceContextInfoMetaData of(PersistenceContext pc,
      PersistenceUnitInfoMetaData unit) {
    return new PersistenceContextInfoMetaData(pc, unit);
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

}
