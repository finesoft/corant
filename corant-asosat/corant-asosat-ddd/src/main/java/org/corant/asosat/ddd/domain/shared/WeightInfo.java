/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.domain.shared;

import java.math.BigDecimal;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

@Embeddable
@MappedSuperclass
public class WeightInfo extends AbstractValueObject {

  private static final long serialVersionUID = 6145301734319644562L;

  private BigDecimal value;

  private String unit;

  protected WeightInfo() {
    super();
  }

  public String getUnit() {
    return unit;
  }

  public BigDecimal getValue() {
    return value;
  }

  protected void setUnit(String unit) {
    this.unit = unit;
  }

  protected void setValue(BigDecimal value) {
    this.value = value;
  }


}
