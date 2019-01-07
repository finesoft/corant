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
package org.corant.asosat.ddd.domain.model;

import static org.corant.kernel.util.Preconditions.requireNotNull;
import static org.corant.suites.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import static org.corant.suites.bundle.GlobalMessageCodes.ERR_SYS;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.shared.util.ConversionUtils;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午1:50:53
 *
 */
@SuppressWarnings("rawtypes")
@Embeddable
@MappedSuperclass
public abstract class AbstractBaseAggregateReference<T extends AbstractBaseGenericAggregate>
    extends AbstractAggregateReference<T> {

  private static final long serialVersionUID = -3880835071968870788L;

  @Transient
  protected transient volatile T referred;

  @Transient
  protected transient boolean holdReferred = true;

  @Column(name = "refId")
  private Long id;

  @Column(name = "refVn")
  private long vn;

  protected AbstractBaseAggregateReference() {}

  protected AbstractBaseAggregateReference(T agg) {
    this.setId(requireNotNull(agg, ERR_OBJ_NON_FUD, "").getId());
    this.setVn(agg.getVn());
    if (this.holdReferred) {
      this.referred = agg;
    }
  }

  protected static <A extends AbstractBaseGenericAggregate, T extends AbstractBaseAggregateReference<A>> T of(
      Object param, Class<T> cls) {
    if (param == null) {
      return null; // FIXME like c++ reference
    }
    try {
      if (param instanceof Long) {
        return ConstructorUtils.invokeExactConstructor(cls, new Object[] {Long.class.cast(param)},
            new Class<?>[] {Long.class});
      } else if (param instanceof String) {
        Long id = ConversionUtils.toLong(param);
        if (id != null) {
          return ConstructorUtils.invokeExactConstructor(cls, new Object[] {id},
              new Class<?>[] {Long.class});
        }
      } else if (param instanceof AbstractBaseAggregateReference
          || param instanceof AbstractBaseGenericAggregate) {
        return ConstructorUtils.invokeExactConstructor(cls, new Object[] {param},
            new Class<?>[] {param.getClass()});
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
        | InstantiationException e) {
      throw new GeneralRuntimeException(e, ERR_OBJ_NON_FUD);
    }
    throw new GeneralRuntimeException(ERR_OBJ_NON_FUD);
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
    AbstractBaseAggregateReference other = (AbstractBaseAggregateReference) obj;
    if (this.id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!this.id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public Long getId() {
    return this.id;
  }

  public long getVn() {
    return this.vn;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.id == null ? 0 : this.id.hashCode());
    return result;
  }

  public Optional<T> optional() {
    return Optional.ofNullable(this.retrieve());
  }

  @Override
  public T retrieve() {
    if (this.holdReferred) {
      if (this.referred == null) {
        synchronized (this) {
          if (this.referred == null) {
            this.referred = super.retrieve();
          }
        }
      }
      return this.referred;
    } else {
      return super.retrieve();
    }
  }

  protected void setId(Long id) {
    this.id = requireNotNull(id, ERR_SYS, id);
  }

  protected void setVn(long vn) {
    this.vn = vn;
  }

}
