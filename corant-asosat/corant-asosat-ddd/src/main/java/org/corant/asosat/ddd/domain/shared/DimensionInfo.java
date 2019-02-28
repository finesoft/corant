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

import static org.corant.kernel.util.Preconditions.requireGt;
import static org.corant.kernel.util.Preconditions.requireNotNull;
import static org.corant.kernel.util.Preconditions.requireTrue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:51:50
 *
 */
@MappedSuperclass
@Embeddable
public class DimensionInfo extends AbstractValueObject {

  private static final long serialVersionUID = -2411775138449357236L;

  @Column
  private BigDecimal height;

  @Column
  private BigDecimal length;

  @Column
  private BigDecimal width;

  @Column(length = 8)
  @Enumerated(EnumType.STRING)
  private MeasureUnit unit;

  public DimensionInfo(BigDecimal length, BigDecimal width, BigDecimal height, MeasureUnit unit) {
    this(length, width, height, unit, false);
  }

  public DimensionInfo(BigDecimal length, BigDecimal width, BigDecimal height, MeasureUnit unit,
      boolean strict) {
    super();
    if (strict) {
      requireNotNull(length, "DimensionInfo.len_error_null");
      requireNotNull(width, "DimensionInfo.width_error_null");
      requireNotNull(height, "DimensionInfo.height_error_null");
      requireNotNull(unit, "DimensionInfo.sizeUnit_error_null");
    }
    setLength(length);
    setWidth(width);
    setHeight(height);
    setUnit(unit);
  }

  public DimensionInfo(DimensionInfo other) {
    this(other.getHeight(), other.getWidth(), other.getHeight(), other.getUnit());
  }

  protected DimensionInfo() {

  }

  public String asDescription() {
    return asDescription(0, "%s x %s x %s %s");
  }

  public String asDescription(int fixScala, String format) {
    return String.format(format,
        length == null ? "?"
            : fixScala < 0 ? length
                : length.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
        width == null ? "?"
            : fixScala < 0 ? width : width.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
        height == null ? "?"
            : fixScala < 0 ? height
                : height.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
        unit == null ? "" : unit.toString());
  }

  /**
   *
   * @param unit
   * @return calVolume
   */
  public BigDecimal calVolume(MeasureUnit unit) {
    return calVolume(unit, null, null, null);
  }

  /**
   * @param unit
   * @param lengthOffset
   * @param widthOffset
   * @param heightOffset
   * @return
   */
  public BigDecimal calVolume(MeasureUnit unit, BigDecimal lengthOffset, BigDecimal widthOffset,
      BigDecimal heightOffset) {
    if (isConsistency()) {
      BigDecimal length = getLength().add(lengthOffset == null ? BigDecimal.ZERO : lengthOffset);
      BigDecimal width = getWidth().add(widthOffset == null ? BigDecimal.ZERO : widthOffset);
      BigDecimal height = getHeight().add(heightOffset == null ? BigDecimal.ZERO : heightOffset);
      if (unit == null || unit == getUnit()) {
        return length.multiply(width).multiply(height);
      } else {
        return getUnit().convert(length, unit).multiply(getUnit().convert(width, unit))
            .multiply(getUnit().convert(height, unit));
      }
    }
    return null;
  }

  public boolean canFit(DimensionInfo other) {
    return canFit(other, null, null, null, null);
  }

  public boolean canFit(DimensionInfo other, BigDecimal lengthOffset, BigDecimal widthOffset,
      BigDecimal heightOffset, MeasureUnit offsetUnit) {
    if (!isConsistency() || other == null || !other.isConsistency()) {
      return false;
    }
    MeasureUnit ofsUn = offsetUnit == null ? getUnit() : offsetUnit;
    BigDecimal lenOfs = lengthOffset == null ? BigDecimal.ZERO : lengthOffset;
    BigDecimal widOfs = lengthOffset == null ? BigDecimal.ZERO : widthOffset;
    BigDecimal higOfs = lengthOffset == null ? BigDecimal.ZERO : heightOffset;
    return getUnit().convert(getLength()).add(ofsUn.convert(lenOfs))
        .compareTo(other.getUnit().convert(other.getLength())) >= 0
        && getUnit().convert(getWidth()).add(ofsUn.convert(widOfs))
            .compareTo(other.getUnit().convert(other.getWidth())) >= 0
        && getUnit().convert(getHeight()).add(ofsUn.convert(higOfs))
            .compareTo(other.getUnit().convert(other.getHeight())) >= 0;
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
    DimensionInfo other = (DimensionInfo) obj;
    if (getHeight() == null) {
      if (other.getHeight() != null) {
        return false;
      }
    } else if (!getHeight().equals(other.getHeight())) {
      return false;
    }
    if (getLength() == null) {
      if (other.getLength() != null) {
        return false;
      }
    } else if (!getLength().equals(other.getLength())) {
      return false;
    }
    if (getUnit() != other.getUnit()) {
      return false;
    }
    if (getWidth() == null) {
      if (other.getWidth() != null) {
        return false;
      }
    } else if (!getWidth().equals(other.getWidth())) {
      return false;
    }
    return true;
  }

  public BigDecimal getHeight() {
    return height;
  }

  public BigDecimal getLength() {
    return length;
  }

  public MeasureUnit getUnit() {
    return unit;
  }

  public BigDecimal getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getHeight() == null ? 0 : getHeight().hashCode());
    result = prime * result + (getLength() == null ? 0 : getLength().hashCode());
    result = prime * result + (getUnit() == null ? 0 : getUnit().hashCode());
    result = prime * result + (getWidth() == null ? 0 : getWidth().hashCode());
    return result;
  }

  @Transient
  public boolean isConsistency() {
    return getLength() != null && getWidth() != null && getHeight() != null && getUnit() != null;
  }

  protected BigDecimal scaleValue(BigDecimal value) {
    return value == null ? null : value.setScale(2, BigDecimal.ROUND_HALF_UP);
  }

  protected void setHeight(BigDecimal height) {
    if (height != null) {
      requireGt(scaleValue(height), BigDecimal.ZERO, "DimensionInfo.len_error_null");
    }
    this.height = scaleValue(height);
  }

  protected void setLength(BigDecimal length) {
    if (length != null) {
      requireGt(scaleValue(length), BigDecimal.ZERO, "DimensionInfo.len_error_null");
    }
    this.length = scaleValue(length);
  }

  protected void setUnit(MeasureUnit unit) {
    if (unit != null) {
      requireTrue(unit.isLength(), "DimensionInfo.sizeUnit_error_null");
    }
    this.unit = unit;
  }

  protected void setWidth(BigDecimal width) {
    if (width != null) {
      requireGt(scaleValue(width), BigDecimal.ZERO, "DimensionInfo.len_error_null");
    }
    this.width = scaleValue(width);
  }
}
