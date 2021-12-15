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

import java.time.LocalDate;
import org.corant.modules.elastic.data.metadata.annotation.EsDate;
import org.corant.modules.elastic.data.metadata.annotation.EsEmbeddable;
import org.corant.modules.elastic.data.metadata.annotation.EsKeyword;
import org.corant.modules.elastic.data.metadata.annotation.EsRange;
import org.corant.modules.elastic.data.metadata.annotation.EsRange.RangeType;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午4:06:37
 *
 */
@EsEmbeddable
public class MixedDateRange {

  public static final MixedDateRange EMPTY_INST = new MixedDateRange(null, null);

  @EsDate
  private LocalDate startValue;

  @EsDate
  private LocalDate endValue;

  @EsRange(type = RangeType.DATE_RANGE)
  private SimpleDateRange rangeValue;

  @EsKeyword
  private String valueKey;

  public MixedDateRange() {}

  public MixedDateRange(LocalDate startValue, LocalDate endValue) {
    setStartValue(startValue);
    setEndValue(endValue);
    setRangeValue(new SimpleDateRange(startValue, endValue));
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
    MixedDateRange other = (MixedDateRange) obj;
    if (getEndValue() == null) {
      if (other.getEndValue() != null) {
        return false;
      }
    } else if (!getEndValue().equals(other.getEndValue())) {
      return false;
    }
    if (getStartValue() == null) {
      if (other.getStartValue() != null) {
        return false;
      }
    } else if (!getStartValue().equals(other.getStartValue())) {
      return false;
    }
    return true;
  }

  public LocalDate getEndValue() {
    return endValue;
  }

  public SimpleDateRange getRangeValue() {
    return rangeValue;
  }

  public LocalDate getStartValue() {
    return startValue;
  }

  public String getValueKey() {
    return valueKey;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getEndValue() == null ? 0 : getEndValue().hashCode());
    return prime * result + (getStartValue() == null ? 0 : getStartValue().hashCode());
  }

  public LocalDate obtainCmprEndValue() {
    return getEndValue() == null ? LocalDate.MAX : getEndValue();
  }

  public LocalDate obtainCmprStartValue() {
    return getStartValue() == null ? LocalDate.MIN : getStartValue();
  }

  private void setEndValue(LocalDate endValue) {
    this.endValue = endValue;
  }

  private void setRangeValue(SimpleDateRange rangeValue) {
    this.rangeValue = rangeValue;
    if (rangeValue != null) {
      setValueKey(rangeValue.toKeyedString());
    }
  }

  private void setStartValue(LocalDate startValue) {
    this.startValue = startValue;
  }

  private void setValueKey(String valueKey) {
    this.valueKey = valueKey;
  }

}
