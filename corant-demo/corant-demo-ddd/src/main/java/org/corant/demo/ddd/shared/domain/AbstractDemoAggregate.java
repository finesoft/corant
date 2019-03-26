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
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractBaseGenericAggregate;
import org.corant.asosat.ddd.domain.shared.Param;

/**
 * corant-demo-ddd
 *
 * @author bingo 下午6:38:50
 *
 */
@MappedSuperclass
public abstract class AbstractDemoAggregate
    extends AbstractBaseGenericAggregate<Param, AbstractDemoAggregate> {

  private static final long serialVersionUID = 4067385038399832270L;

  @Column
  private String strVal;

  @Column
  private BigDecimal decVal;

  @Column
  private Boolean boolVal;

  @Column
  private Long timeVal;

  @Embedded
  private DemoValue oneVal;

  /**
   *
   * @return the boolVal
   */
  public Boolean getBoolVal() {
    return boolVal;
  }

  /**
   *
   * @return the decVal
   */
  public BigDecimal getDecVal() {
    return decVal;
  }

  /**
   *
   * @return the oneVal
   */
  public DemoValue getOneVal() {
    return oneVal;
  }

  /**
   *
   * @return the strVal
   */
  public String getStrVal() {
    return strVal;
  }

  /**
   *
   * @return the timeVal
   */
  public Long getTimeVal() {
    return timeVal;
  }

  /**
   *
   * @param boolVal the boolVal to set
   */
  public void setBoolVal(Boolean boolVal) {
    this.boolVal = boolVal;
  }

  /**
   *
   * @param decVal the decVal to set
   */
  public void setDecVal(BigDecimal decVal) {
    this.decVal = decVal;
  }

  /**
   *
   * @param oneVal the oneVal to set
   */
  public void setOneVal(DemoValue oneVal) {
    this.oneVal = oneVal;
  }

  /**
   *
   * @param strVal the strVal to set
   */
  public void setStrVal(String strVal) {
    this.strVal = strVal;
  }

  /**
   *
   * @param timeVal the timeVal to set
   */
  public void setTimeVal(Long timeVal) {
    this.timeVal = timeVal;
  }

}
