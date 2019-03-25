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
package org.corant.asosat.ddd.domain.model;

import static org.corant.kernel.util.Preconditions.requireNotNull;
import static org.corant.suites.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午1:50:53
 *
 */
@SuppressWarnings("rawtypes")
@Embeddable
@MappedSuperclass
public abstract class AbstractVersionedAggregateReference<T extends AbstractBaseGenericAggregate>
    extends AbstractBaseAggregateReference<T> {

  private static final long serialVersionUID = -3880835071968870788L;

  @Column(name = "refVn")
  private long vn;

  protected AbstractVersionedAggregateReference() {}

  protected AbstractVersionedAggregateReference(T agg) {
    setId(requireNotNull(agg, ERR_OBJ_NON_FUD, "").getId());
    this.setVn(agg.getVn());
    if (holdReferred) {
      referred = agg;
    }
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
    AbstractVersionedAggregateReference other = (AbstractVersionedAggregateReference) obj;
    if (vn != other.vn) {
      return false;
    }
    return true;
  }

  public long getVn() {
    return this.vn;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (int) (vn ^ vn >>> 32);
    return result;
  }

  @Override
  public Optional<T> optional() {
    return Optional.ofNullable(this.retrieve());
  }

  protected void setVn(long vn) {
    this.vn = vn;
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}
