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

import static java.math.BigDecimal.ONE;
import java.math.BigDecimal;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:45:40
 *
 */
public enum MeasureUnit {

  //@formatter:off
  MM(new BigDecimal("0.001"), MeasureUnitType.LENGTH),
  CM(new BigDecimal("0.01"), MeasureUnitType.LENGTH),
  M(ONE, MeasureUnitType.LENGTH),

  KG(ONE, MeasureUnitType.WEIGHT),
  TON(new BigDecimal("1000"), MeasureUnitType.WEIGHT),
  G(new BigDecimal("0.001"), MeasureUnitType.WEIGHT),

  CBM(ONE, MeasureUnitType.VOLUME);
  //@formatter:on

  private BigDecimal coefficient;

  private MeasureUnitType type;

  private MeasureUnit(BigDecimal v, MeasureUnitType ut) {
    coefficient = v;
    type = ut;
  }

  public BigDecimal convert(BigDecimal val) {
    return coefficient.multiply(val);
  }

  public BigDecimal convert(BigDecimal val, MeasureUnit target) {
    return convert(val).divide(target.coefficient);
  }

  public MeasureUnitType getType() {
    return type;
  }

  public boolean isLength() {
    return MeasureUnitType.LENGTH.equals(getType());
  }

  public boolean isVolume() {
    return MeasureUnitType.VOLUME.equals(getType());
  }

  public boolean isWeight() {
    return MeasureUnitType.WEIGHT.equals(getType());
  }

  @Converter
  public static class MeasureUnitJPAConverter implements AttributeConverter<MeasureUnit, String> {
    @Override
    public String convertToDatabaseColumn(MeasureUnit attribute) {
      return attribute.name();
    }

    @Override
    public MeasureUnit convertToEntityAttribute(String dbData) {
      return MeasureUnit.valueOf(dbData);
    }
  }

  public static enum MeasureUnitType {
    LENGTH, WEIGHT, VOLUME
  }
}
