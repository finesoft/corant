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
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.defaultString;
import java.beans.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:16:41
 *
 */
@MappedSuperclass
@Embeddable
public class PackagingLayout extends AbstractValueObject {

  private static final long serialVersionUID = -5202808097803594548L;

  @Column
  private BigDecimal qty;

  @Column(length = 8)
  private String unit;

  public PackagingLayout() {

  }

  /**
   * @param qty
   * @param pu
   */
  public PackagingLayout(BigDecimal qty, String unit) {
    super();
    setQty(qty);
    setUnit(unit);
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
    PackagingLayout other = (PackagingLayout) obj;
    if (getQty() == null) {
      if (other.getQty() != null) {
        return false;
      }
    } else if (!getQty().equals(other.getQty())) {
      return false;
    }
    if (getUnit() == null) {
      if (other.getUnit() != null) {
        return false;
      }
    } else if (!getUnit().equals(other.getUnit())) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the qty
   */
  public BigDecimal getQty() {
    return qty;
  }

  /**
   *
   * @return the pu
   */
  public String getUnit() {
    return unit;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getQty() == null ? 0 : getQty().hashCode());
    result = prime * result + (getUnit() == null ? 0 : getUnit().hashCode());
    return result;
  }

  /**
   *
   * @param qty the qty to set
   */
  protected void setQty(BigDecimal qty) {
    this.qty = Measurables.defaultScale(qty);
  }

  /**
   *
   * @param pu the pu to set
   */
  protected void setUnit(String unit) {
    this.unit = unit;
  }

  @MappedSuperclass
  @Embeddable
  public static class RegularPackagingLayout extends PackagingLayout {

    private static final long serialVersionUID = 5330074295611830695L;

    @Column
    private BigDecimal lengthQty;

    @Column
    private BigDecimal widthQty;

    @Column
    private BigDecimal heightQty;

    public RegularPackagingLayout() {
      super();
    }

    /**
     * @param lengthQty
     * @param widthQty
     * @param heightQty
     */
    public RegularPackagingLayout(BigDecimal lengthQty, BigDecimal widthQty, BigDecimal heightQty,
        String unit) {
      super();
      setLengthQty(lengthQty);
      setWidthQty(widthQty);
      setHeightQty(heightQty);
      setUnit(unit);
    }

    /**
     * @param qty
     * @param pu
     */
    public RegularPackagingLayout(BigDecimal qty, String unit) {
      super(qty, unit);
    }

    public static RegularPackagingLayout single(String unit) {
      return new RegularPackagingLayout(ONE, ONE, ONE, unit);
    }

    public String asDescription() {
      return asDescription(0, "%s x %s x %s %s");
    }

    public String asDescription(int fixScala, String format) {
      return String.format(format,
          lengthQty == null ? "?"
              : fixScala < 0 ? lengthQty
                  : lengthQty.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
          widthQty == null ? "?"
              : fixScala < 0 ? widthQty
                  : widthQty.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
          heightQty == null ? "?"
              : fixScala < 0 ? heightQty
                  : heightQty.setScale(fixScala, RoundingMode.HALF_UP).toPlainString(),
          defaultString(getUnit()));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      RegularPackagingLayout other = (RegularPackagingLayout) obj;
      if (heightQty == null) {
        if (other.heightQty != null) {
          return false;
        }
      } else if (!heightQty.equals(other.heightQty)) {
        return false;
      }
      if (lengthQty == null) {
        if (other.lengthQty != null) {
          return false;
        }
      } else if (!lengthQty.equals(other.lengthQty)) {
        return false;
      }
      if (widthQty == null) {
        if (other.widthQty != null) {
          return false;
        }
      } else if (!widthQty.equals(other.widthQty)) {
        return false;
      }
      return true;
    }

    @javax.persistence.Transient
    @Transient
    public BigDecimal get2DQty() {
      if (getLengthQty() != null && getWidthQty() != null) {
        return getLengthQty().multiply(getWidthQty());
      }
      return null;
    }

    /**
     *
     * @return the heightQty
     */
    public BigDecimal getHeightQty() {
      return heightQty;
    }

    /**
     *
     * @return the lengthQty
     */
    public BigDecimal getLengthQty() {
      return lengthQty;
    }

    /**
     *
     * @return the widthQty
     */
    public BigDecimal getWidthQty() {
      return widthQty;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (heightQty == null ? 0 : heightQty.hashCode());
      result = prime * result + (lengthQty == null ? 0 : lengthQty.hashCode());
      result = prime * result + (widthQty == null ? 0 : widthQty.hashCode());
      return result;
    }

    @javax.persistence.Transient
    @Transient
    public boolean isConsistency() {
      return getLengthQty() != null && getWidthQty() != null && getHeightQty() != null
          && getUnit() != null;
    }

    /**
     *
     * @param heightQty the heightQty to set
     */
    protected void setHeightQty(BigDecimal heightQty) {
      this.heightQty = Measurables.defaultScale(heightQty);
      initQty();
    }

    /**
     *
     * @param lengthQty the lengthQty to set
     */
    protected void setLengthQty(BigDecimal lengthQty) {
      this.lengthQty = Measurables.defaultScale(lengthQty);
      initQty();
    }

    /**
     *
     * @param widthQty the widthQty to set
     */
    protected void setWidthQty(BigDecimal widthQty) {
      this.widthQty = Measurables.defaultScale(widthQty);
      initQty();
    }

    void initQty() {
      if (lengthQty != null || widthQty != null || heightQty != null) {
        setQty(defaultObject(lengthQty, ONE).multiply(defaultObject(widthQty, ONE))
            .multiply(defaultObject(heightQty, ONE)));
      }
    }

  }

}
