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
package org.corant.modules.dsa.cluster.kmeans;

import java.util.Map;

/**
 * corant-modules-dsa
 *
 * @author bingo 下午2:10:43
 *
 */
public class Centroid {

  private final Map<Object, Double> coordinates;

  public Centroid(Map<Object, Double> coordinates) {
    this.coordinates = coordinates;
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
    Centroid other = (Centroid) obj;
    if (coordinates == null) {
      if (other.coordinates != null) {
        return false;
      }
    } else if (!coordinates.equals(other.coordinates)) {
      return false;
    }
    return true;
  }

  public Map<Object, Double> getCoordinates() {
    return coordinates;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (coordinates == null ? 0 : coordinates.hashCode());
  }

  @Override
  public String toString() {
    return "Centroid " + coordinates;
  }

}
