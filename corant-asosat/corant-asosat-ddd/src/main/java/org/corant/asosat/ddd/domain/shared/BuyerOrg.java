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
package org.corant.asosat.ddd.domain.shared;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

@Embeddable
@MappedSuperclass
@AttributeOverrides(
    value = {@AttributeOverride(column = @Column(name = "buyerOrgId", length = 36), name = "id"),
        @AttributeOverride(column = @Column(name = "buyerOrgName"), name = "name")})
public class BuyerOrg extends Participator {

  private static final long serialVersionUID = 2482047799269041296L;

  /**
   * @param obj
   */
  public BuyerOrg(Object obj) {
    super(obj);
  }

  /**
   * @param id
   * @param name
   */
  public BuyerOrg(String id, String name) {
    super(id, name);
  }

  /**
   *
   */
  protected BuyerOrg() {
    super();
  }

  @Override
  protected void setId(String id) {
    super.setId(id);
  }

  @Override
  protected void setName(String name) {
    super.setName(name);
  }

}
