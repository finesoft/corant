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
package org.corant.modules.elastic.data.model;

import java.time.LocalDate;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbeddable;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午4:07:01
 *
 */
@EsEmbeddable
public class SimpleDateRange {

  private LocalDate gte;

  private LocalDate lte;

  public SimpleDateRange(LocalDate gte, LocalDate lte) {
    setGte(gte);
    setLte(lte);
  }

  protected SimpleDateRange() {

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
    SimpleDateRange other = (SimpleDateRange) obj;
    if (getGte() == null) {
      if (other.getGte() != null) {
        return false;
      }
    } else if (!getGte().equals(other.getGte())) {
      return false;
    }
    if (getLte() == null) {
      if (other.getLte() != null) {
        return false;
      }
    } else if (!getLte().equals(other.getLte())) {
      return false;
    }
    return true;
  }

  public LocalDate getGte() {
    return gte;
  }

  public LocalDate getLte() {
    return lte;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getGte() == null ? 0 : getGte().hashCode());
    return prime * result + (getLte() == null ? 0 : getLte().hashCode());
  }

  public String toKeyedString() {
    StringBuilder sb = new StringBuilder();
    if (getGte() != null) {
      sb.append(getGte().toString());
    } else {
      sb.append("?");
    }
    if (getLte() != null) {
      sb.append(" - ").append(getLte());
    } else {
      sb.append("->");
    }
    return sb.toString();
  }

  private void setGte(LocalDate gte) {
    this.gte = gte;
  }

  private void setLte(LocalDate lte) {
    this.lte = lte;
  }

}
