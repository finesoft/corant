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
package org.corant.asosat.ddd.domain.shared;

import java.time.Instant;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

/**
 * @author bingo 下午7:52:57
 *
 */
@Embeddable
@MappedSuperclass
public class CreationInfo implements OperationInfo {

  private static final long serialVersionUID = 8168649121736024097L;

  static final CreationInfo EMPTY_INST = new CreationInfo();

  @Embedded
  @AttributeOverrides(value = {
      @AttributeOverride(column = @Column(name = "creatorId", updatable = false), name = "id"),
      @AttributeOverride(column = @Column(name = "creatorName", length = 320, updatable = false),
          name = "name")})
  private Participator creator;

  @Column(name = "createdTime", updatable = false)
  private Instant createdTime;

  public CreationInfo(Participator creator) {
    this(creator, Instant.now());
  }

  public CreationInfo(Participator creator, Instant createdTime) {
    super();
    this.creator = creator;
    this.createdTime = createdTime;
  }

  protected CreationInfo() {}

  public static CreationInfo empty() {
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
    CreationInfo other = (CreationInfo) obj;
    if (createdTime == null) {
      if (other.createdTime != null) {
        return false;
      }
    } else if (!createdTime.equals(other.createdTime)) {
      return false;
    }
    if (creator == null) {
      if (other.creator != null) {
        return false;
      }
    } else if (!creator.equals(other.creator)) {
      return false;
    }
    return true;
  }

  public Instant getCreatedTime() {
    return createdTime;
  }

  public Participator getCreator() {
    return creator == null ? Participator.EMPTY_INST : creator;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (createdTime == null ? 0 : createdTime.hashCode());
    result = prime * result + (creator == null ? 0 : creator.hashCode());
    return result;
  }

  @Override
  public Instant obtainOperatedTime() {
    return getCreatedTime();
  }

  @Override
  public Long obtainOperatorId() {
    return getCreator().getId();
  }

}

