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
package org.corant.demo.ddd.shared.domain;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

/**
 * corant-demo-ddd
 *
 * @author bingo 下午6:41:55
 *
 */
@MappedSuperclass
@Embeddable
public class DemoValue extends AbstractValueObject {

  private static final long serialVersionUID = -1035125580254801949L;

  @Column
  private String someStrVal;

  @Column
  private BigDecimal someDecVal;

  @Column
  private Boolean someBoolVal;

  /**
   * @param someStrVal
   * @param someDecVal
   * @param someBoolVal
   */
  public DemoValue(String someStrVal, BigDecimal someDecVal, Boolean someBoolVal) {
    super();
    this.someStrVal = someStrVal;
    this.someDecVal = someDecVal;
    this.someBoolVal = someBoolVal;
  }

  protected DemoValue() {}

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
    DemoValue other = (DemoValue) obj;
    if (someBoolVal == null) {
      if (other.someBoolVal != null) {
        return false;
      }
    } else if (!someBoolVal.equals(other.someBoolVal)) {
      return false;
    }
    if (someDecVal == null) {
      if (other.someDecVal != null) {
        return false;
      }
    } else if (!someDecVal.equals(other.someDecVal)) {
      return false;
    }
    if (someStrVal == null) {
      if (other.someStrVal != null) {
        return false;
      }
    } else if (!someStrVal.equals(other.someStrVal)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the someBoolVal
   */
  public Boolean getSomeBoolVal() {
    return someBoolVal;
  }

  /**
   *
   * @return the someDecVal
   */
  public BigDecimal getSomeDecVal() {
    return someDecVal;
  }

  /**
   *
   * @return the someStrVal
   */
  public String getSomeStrVal() {
    return someStrVal;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (someBoolVal == null ? 0 : someBoolVal.hashCode());
    result = prime * result + (someDecVal == null ? 0 : someDecVal.hashCode());
    result = prime * result + (someStrVal == null ? 0 : someStrVal.hashCode());
    return result;
  }

}
