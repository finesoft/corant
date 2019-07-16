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
package org.corant.asosat.ddd.domain.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午5:17:22
 *
 */
@SuppressWarnings("rawtypes")
@Embeddable
@MappedSuperclass
public abstract class AbstractReferenceDataAggregationReference<T extends AbstractReferenceDataAggregation>
    extends AbstractVersionedAggregationReference<T> {

  private static final long serialVersionUID = -7976294790832075954L;

  @Column(name = "refName")
  private String name;

  @Column(name = "refNumber")
  private String number;

  public AbstractReferenceDataAggregationReference(T agg) {
    super(agg);
    this.setName(agg.getName());
    this.setNumber(agg.getNumber());
  }

  protected AbstractReferenceDataAggregationReference() {
    super();
  }

  public String getName() {
    return this.name;
  }

  public String getNumber() {
    return this.number;
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setNumber(String number) {
    this.number = number;
  }



}
