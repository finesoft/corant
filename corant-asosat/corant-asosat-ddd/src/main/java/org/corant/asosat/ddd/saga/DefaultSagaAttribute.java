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
package org.corant.asosat.ddd.saga;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.AttributeType;
import org.corant.shared.util.ConversionUtils;
import org.corant.suites.ddd.model.Value;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:19:36
 *
 */
@MappedSuperclass
@Embeddable
public class DefaultSagaAttribute implements SagaAttribute, Value {

  private static final long serialVersionUID = -8719994616061589039L;

  @Column
  private String name;

  @Column
  private AttributeType type;

  @Column
  private Boolean boolValue;

  @Column
  private BigDecimal numberValue;

  @Column
  private String stringValue;

  @Column
  private ZonedDateTime temporalValue;

  public DefaultSagaAttribute(String name, Boolean boolValue) {
    setType(AttributeType.BOOLEAN);
    setName(name);
    setBoolValue(ConversionUtils.toBoolean(boolValue));
  }

  public DefaultSagaAttribute(String name, Number numberValue) {
    setType(AttributeType.NUMBERIC);
    setName(name);
    setNumberValue(ConversionUtils.toBigDecimal(numberValue));
  }

  public DefaultSagaAttribute(String name, String stringValue) {
    setType(AttributeType.STRING);
    setName(name);
    setStringValue(stringValue);
  }

  public DefaultSagaAttribute(String name, Temporal temporalValue) {
    setType(AttributeType.TEMPORAL);
    setName(name);
    setTemporalValue(ConversionUtils.toZonedDateTime(temporalValue));
  }

  protected DefaultSagaAttribute() {}

  @Override
  public Boolean getBoolValue() {
    return boolValue;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public BigDecimal getNumberValue() {
    return numberValue;
  }

  @Override
  public String getStringValue() {
    return stringValue;
  }

  @Override
  public ZonedDateTime getTemporalValue() {
    return temporalValue;
  }

  @Override
  public AttributeType getType() {
    return type;
  }

  protected void setBoolValue(Boolean boolValue) {
    this.boolValue = boolValue;
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setNumberValue(BigDecimal numberValue) {
    this.numberValue = numberValue;
  }

  protected void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }

  protected void setTemporalValue(ZonedDateTime temporalValue) {
    this.temporalValue = temporalValue;
  }

  protected void setType(AttributeType type) {
    this.type = type;
  }

}
