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
package org.corant.suites.dsa.cluster.kmeans;

import static org.corant.shared.util.ObjectUtils.asString;
import java.util.Map;

/**
 * corant-suites-dsa
 *
 * @author bingo 下午2:12:15
 *
 */
public class Record {

  private final Object id;

  private final Map<Object, Double> features;

  public Record(Map<Object, Double> features) {
    this("", features);
  }

  public Record(Object id, Map<Object, Double> features) {
    this.id = id;
    this.features = features;
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
    Record other = (Record) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (features == null) {
      if (other.features != null) {
        return false;
      }
    } else if (!features.equals(other.features)) {
      return false;
    }
    return true;
  }

  /**
   * Encapsulates all attributes and their corresponding values, i.e. features.
   */
  public Map<Object, Double> getFeatures() {
    return features;
  }

  /**
   * The record id example.
   */
  public Object getId() {
    return id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (id == null ? 0 : id.hashCode());
    result = prime * result + (features == null ? 0 : features.hashCode());
    return result;
  }

  @Override
  public String toString() {
    String prefix = id == null ? "Record" : asString(id);
    return prefix + ": " + features;
  }
}
