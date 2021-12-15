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

import static org.corant.shared.util.Conversions.toBigDecimal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbeddable;
import org.corant.modules.elastic.data.metadata.annotation.EsKeyword;
import org.corant.modules.elastic.data.metadata.annotation.EsNumeric;
import org.corant.modules.elastic.data.metadata.annotation.EsNumeric.EsNumericType;
import org.corant.modules.elastic.data.metadata.annotation.EsRange;
import org.corant.modules.elastic.data.metadata.annotation.EsRange.RangeType;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午4:06:50
 *
 */
@EsEmbeddable
public class MixedFloatRange {

  public static final MixedFloatRange EMPTY_INST = new MixedFloatRange(null, null);

  @EsNumeric(type = EsNumericType.FLOAT)
  private BigDecimal maxValue;

  @EsNumeric(type = EsNumericType.FLOAT)
  private BigDecimal minValue;

  @EsRange(type = RangeType.FLOAT_RANGE)
  private SimpleFloatRange rangeValue;

  @EsKeyword
  private String valueKey;

  public MixedFloatRange() {

  }

  public MixedFloatRange(BigDecimal minValue, BigDecimal maxValue) {
    setMinValue(minValue);
    setMaxValue(maxValue);
    setRangeValue(new SimpleFloatRange(minValue, maxValue));
  }

  public MixedFloatRange(Object minValue, Object maxValue) {
    this(toBigDecimal(minValue), toBigDecimal(maxValue));
  }

  public MixedFloatRange(SimpleFloatRange obj) {
    if (obj != null) {
      setMinValue(obj.getGte());
      setMaxValue(obj.getLte());
      setRangeValue(obj);
    }
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
    MixedFloatRange other = (MixedFloatRange) obj;
    if (maxValue == null) {
      if (other.maxValue != null) {
        return false;
      }
    } else if (!maxValue.equals(other.maxValue)) {
      return false;
    }
    if (minValue == null) {
      if (other.minValue != null) {
        return false;
      }
    } else if (!minValue.equals(other.minValue)) {
      return false;
    }
    return true;
  }

  public BigDecimal getMaxValue() {
    return maxValue;
  }

  public BigDecimal getMinValue() {
    return minValue;
  }

  public SimpleFloatRange getRangeValue() {
    return rangeValue;
  }

  public String getValueKey() {
    return valueKey;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (maxValue == null ? 0 : maxValue.hashCode());
    return prime * result + (minValue == null ? 0 : minValue.hashCode());
  }

  public BigDecimal midValue() {
    BigDecimal v = null;
    if (getMinValue() != null) {
      v = getMinValue();
    }
    if (getMaxValue() != null) {
      if (v == null) {
        v = getMaxValue();
      } else {
        v = v.add(getMaxValue()).divide(BigDecimal.valueOf(2));
      }
    }
    return v == null ? null : BigDecimal.valueOf(v.doubleValue()).setScale(2, RoundingMode.HALF_UP);
  }

  private void setMaxValue(BigDecimal maxValue) {
    this.maxValue = maxValue == null ? null : maxValue.setScale(2, RoundingMode.HALF_UP);
  }

  private void setMinValue(BigDecimal minValue) {
    this.minValue = minValue == null ? null : minValue.setScale(2, RoundingMode.HALF_UP);
  }

  private void setRangeValue(SimpleFloatRange rangeValue) {
    this.rangeValue = rangeValue;
    if (rangeValue != null) {
      setValueKey(rangeValue.toKeyedString());
    }
  }

  private void setValueKey(String valueKey) {
    this.valueKey = valueKey;
  }

}
