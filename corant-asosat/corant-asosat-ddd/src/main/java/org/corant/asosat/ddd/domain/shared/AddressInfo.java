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

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

@Embeddable
@MappedSuperclass
public class AddressInfo extends AbstractValueObject {

  private static final long serialVersionUID = -9085295280837794048L;

  private Long locationId;

  private long locationVn;

  private String locationName;

  private String description;

  protected AddressInfo() {
    super();
  }

  public String getDescription() {
    return description;
  }

  public Long getLocationId() {
    return locationId;
  }

  public String getLocationName() {
    return locationName;
  }

  public long getLocationVn() {
    return locationVn;
  }

  /**
   *
   * @param description the description to set
   */
  protected void setDescription(String description) {
    this.description = description;
  }

  /**
   *
   * @param locationId the locationId to set
   */
  protected void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  /**
   *
   * @param locationName the locationName to set
   */
  protected void setLocationName(String locationName) {
    this.locationName = locationName;
  }

  /**
   *
   * @param locationVn the locationVn to set
   */
  protected void setLocationVn(long locationVn) {
    this.locationVn = locationVn;
  }

}
