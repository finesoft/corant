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

import static org.corant.kernel.util.Preconditions.requireTrue;
import static org.corant.shared.util.ConversionUtils.toBigDecimal;
import static org.corant.shared.util.ObjectUtils.asString;
import java.beans.Transient;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:21:27
 *
 */
public class Measurables {

  public static final int DECIMAL_SCALE = ConfigProvider.getConfig()
      .getOptionalValue("global.decimal.default-scale", Integer.class).orElse(2);
  public static final int DECIMAL_ROUND = ConfigProvider.getConfig()
      .getOptionalValue("global.decimal.default-round", Integer.class).orElse(4);

  public static BigDecimal defaultScale(BigDecimal value) {
    return value == null ? null : value.setScale(DECIMAL_SCALE, DECIMAL_ROUND);
  }

  public static void validateComparables(Measured<?>... measurables) {
    for (Measured<?> m : measurables) {
      if (m == null || !m.isComparable()) {
        throw new NotComparableException();
      }
    }
  }

  public interface Measured<T extends Measured<T>> extends Comparable<T> {

    T add(T other);

    @Transient
    @javax.persistence.Transient
    default BigDecimal defaultScale(BigDecimal value) {
      return Measurables.defaultScale(value);
    }

    T divide(BigDecimal divisor);

    MeasureUnit getUnit();

    BigDecimal getValue();

    @Transient
    @javax.persistence.Transient
    default boolean isComparable() {
      return false;
    }

    T multiply(BigDecimal multiplicand);

    T subtract(T other);

    T withUnit(MeasureUnit unit);
  }

  @Embeddable
  @MappedSuperclass
  public static abstract class MeasuredInfo<T extends MeasuredInfo<T>> extends AbstractValueObject
      implements Measured<T> {

    private static final long serialVersionUID = 6145301734319644562L;

    @Column
    private BigDecimal value;

    @Column(length = 8)
    @Enumerated(EnumType.STRING)
    private MeasureUnit unit;

    /**
     * @param value
     * @param unit
     */
    public MeasuredInfo(BigDecimal value, MeasureUnit unit) {
      super();
      setValue(value);
      setUnit(unit);
    }

    /**
     * @param value
     * @param unit
     */
    public MeasuredInfo(Number value, MeasureUnit unit) {
      super();
      setValue(toBigDecimal(value));
      setUnit(unit);
    }

    protected MeasuredInfo() {
      super();
    }

    @Override
    public T add(T other) {
      Measurables.validateComparables(other, this);
      return with(defaultScale(value.add(other.getUnit().convert(other.getValue(), getUnit()))),
          getUnit());
    }

    @Override
    public int compareTo(T other) {
      Measurables.validateComparables(other, this);
      return value.compareTo(other.getUnit().convert(other.getValue(), getUnit()));
    }

    @Override
    public T divide(BigDecimal divisor) {
      Measurables.validateComparables(this);
      return with(Measurables.defaultScale(getValue().divide(divisor)), getUnit());
    }

    @SuppressWarnings("rawtypes")
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
      MeasuredInfo other = (MeasuredInfo) obj;
      if (unit != other.unit) {
        return false;
      }
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }

    @Override
    public MeasureUnit getUnit() {
      return unit;
    }

    @Override
    public BigDecimal getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (unit == null ? 0 : unit.hashCode());
      result = prime * result + (value == null ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean isComparable() {
      return value != null && getUnit() != null;
    }

    @Override
    public T multiply(BigDecimal multiplicand) {
      Measurables.validateComparables(this);
      return with(Measurables.defaultScale(getValue().multiply(multiplicand)), getUnit());
    }

    @Override
    public T subtract(T other) {
      Measurables.validateComparables(other, this);
      return with(
          defaultScale(value.subtract(other.getUnit().convert(other.getValue(), getUnit()))),
          getUnit());
    }

    @Override
    public String toString() {
      return asString(value) + " (" + asString(unit) + ")";
    }

    @Override
    public T withUnit(MeasureUnit unit) {
      Measurables.validateComparables(this);
      requireTrue(unit != null && unit.isLength(), GlobalMessageCodes.ERR_PARAM);
      return with(defaultScale(getUnit().convert(getValue(), unit)), unit);
    }

    protected abstract MeasureUnit checkUnitType(MeasureUnit unit);

    protected void setUnit(MeasureUnit unit) {
      this.unit = checkUnitType(unit);
    }

    protected void setValue(BigDecimal value) {
      this.value = defaultScale(value);
    }

    protected abstract T with(BigDecimal value, MeasureUnit unit);
  }

  public static class NotComparableException extends CorantRuntimeException {

    private static final long serialVersionUID = 2785024371127444242L;

    protected NotComparableException() {
      super();
    }

    protected NotComparableException(String msgOrFormat, Object... args) {
      super(msgOrFormat, args);
    }

    protected NotComparableException(Throwable cause) {
      super(cause);
    }

    protected NotComparableException(Throwable cause, boolean enableSuppression,
        boolean writableStackTrace, String msgOrFormat, Object... args) {
      super(cause, enableSuppression, writableStackTrace, msgOrFormat, args);
    }

    protected NotComparableException(Throwable cause, String msgOrFormat, Object... args) {
      super(cause, msgOrFormat, args);
    }

  }

}
