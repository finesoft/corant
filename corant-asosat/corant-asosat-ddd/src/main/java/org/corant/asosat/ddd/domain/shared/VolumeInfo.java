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

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

@Embeddable
@MappedSuperclass
public class VolumeInfo extends AbstractValueObject implements Comparable<VolumeInfo> {

  private static final long serialVersionUID = 6145301734319644562L;

  static final VolumeInfo VI0 = new VolumeInfo(BigDecimal.ZERO, MeasureUnit.KG);

  @Column
  private BigDecimal volume;

  @Column(length = 8)
  @Enumerated(EnumType.STRING)
  private MeasureUnit unit;

  /**
   * @param volume
   * @param unit
   */
  public VolumeInfo(BigDecimal volume, MeasureUnit unit) {
    super();
    this.volume = volume;
    this.unit = unit;
  }

  protected VolumeInfo() {
    super();
  }

  public static VolumeInfo of(BigDecimal volume, MeasureUnit unit) {
    return new VolumeInfo(volume, unit);
  }

  @Override
  public int compareTo(VolumeInfo o) {
    return defaultObject(volume, BigDecimal.ZERO)
        .compareTo(defaultObject(o.volume, BigDecimal.ZERO));
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
    VolumeInfo other = (VolumeInfo) obj;
    if (unit != other.unit) {
      return false;
    }
    if (volume == null) {
      if (other.volume != null) {
        return false;
      }
    } else if (!volume.equals(other.volume)) {
      return false;
    }
    return true;
  }

  public MeasureUnit getUnit() {
    return unit;
  }

  public BigDecimal getVolume() {
    return volume;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (unit == null ? 0 : unit.hashCode());
    result = prime * result + (volume == null ? 0 : volume.hashCode());
    return result;
  }

  protected void setUnit(MeasureUnit unit) {
    this.unit = unit;
  }

  protected void setVolume(BigDecimal volume) {
    this.volume = volume;
  }

}
