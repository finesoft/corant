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

import java.time.Instant;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

/**
 * @author bingo 下午7:51:43
 *
 */
@Embeddable
@MappedSuperclass
public class ModificationInfo implements OperationInfo {

  private static final long serialVersionUID = -5680557426023292137L;

  static final ModificationInfo EMPTY_INST = new ModificationInfo();

  @Embedded
  @AttributeOverrides(
      value = {@AttributeOverride(column = @Column(name = "modifierId"), name = "id"),
          @AttributeOverride(column = @Column(name = "modifierName", length = 320), name = "name")})
  private Participator modifier;

  @Column(name = "modifiedTime")
  private Instant modifiedTime;

  public ModificationInfo(Participator modifier) {
    this(modifier, Instant.now());
  }

  public ModificationInfo(Participator modifier, Instant modifiedTime) {
    super();
    this.modifier = modifier;
    this.modifiedTime = modifiedTime;
  }

  protected ModificationInfo() {}

  public static ModificationInfo empty() {
    return EMPTY_INST;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    ModificationInfo other = (ModificationInfo) obj;
    if (modifiedTime == null) {
      if (other.modifiedTime != null) {
        return false;
      }
    } else if (!modifiedTime.equals(other.modifiedTime)) {
      return false;
    }
    if (modifier == null) {
      if (other.modifier != null) {
        return false;
      }
    } else if (!modifier.equals(other.modifier)) {
      return false;
    }
    return true;
  }

  public Instant getModifiedTime() {
    return modifiedTime;
  }

  public Participator getModifier() {
    return modifier == null ? Participator.EMPTY_INST : modifier;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (modifiedTime == null ? 0 : modifiedTime.hashCode());
    result = prime * result + (modifier == null ? 0 : modifier.hashCode());
    return result;
  }

  @Override
  public Instant obtainOperatedTime() {
    return getModifiedTime();
  }

  @Override
  public String obtainOperatorId() {
    return getModifier().getId();
  }

}
