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
import java.math.BigDecimal;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.shared.Measurables.MeasuredInfo;
import org.corant.suites.bundle.GlobalMessageCodes;

@Embeddable
@MappedSuperclass
public class VolumeInfo extends MeasuredInfo<VolumeInfo> {

  private static final long serialVersionUID = 6145301734319644562L;

  public static final VolumeInfo VI0 = new VolumeInfo(BigDecimal.ZERO, MeasureUnit.CBM);

  /**
   * @param value
   * @param unit
   */
  public VolumeInfo(BigDecimal value, MeasureUnit unit) {
    super(value, unit);
  }

  /**
   *
   */
  protected VolumeInfo() {
    super();
  }

  @Override
  protected MeasureUnit checkUnitType(MeasureUnit unit) {
    if (unit != null) {
      requireTrue(unit.isVolume(), GlobalMessageCodes.ERR_PARAM);
    }
    return unit;
  }

  @Override
  protected VolumeInfo with(BigDecimal value, MeasureUnit unit) {
    return new VolumeInfo(value, unit);
  }

}
