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
package org.corant.suites.elastic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.corant.suites.elastic.metadata.annotation.EsEmbeddable;

@EsEmbeddable
public class SimpleFloatRange {

  private BigDecimal gte;

  private BigDecimal lte;

  public SimpleFloatRange(BigDecimal gte, BigDecimal lte) {
    super();
    setGte(gte);
    setLte(lte);
  }

  protected SimpleFloatRange() {

  }

  public BigDecimal advValue() {
    BigDecimal v = null;
    if (getGte() != null) {
      v = getGte();
    }
    if (getLte() != null) {
      if (v == null) {
        v = getLte();
      } else {
        v = v.add(getLte()).divide(BigDecimal.valueOf(2));
      }
    }
    return v == null ? null : BigDecimal.valueOf(v.doubleValue()).setScale(2, RoundingMode.HALF_UP);
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
    SimpleFloatRange other = (SimpleFloatRange) obj;
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

  public BigDecimal getGte() {
    return gte;
  }

  public BigDecimal getLte() {
    return lte;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getGte() == null ? 0 : getGte().hashCode());
    result = prime * result + (getLte() == null ? 0 : getLte().hashCode());
    return result;
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
      sb.append(" - ?");
    }
    return sb.toString();
  }

  private void setGte(BigDecimal gte) {
    this.gte = gte == null ? null : gte.setScale(2, RoundingMode.HALF_UP);
  }

  private void setLte(BigDecimal lte) {
    this.lte = lte == null ? null : lte.setScale(2, RoundingMode.HALF_UP);
  }

}
