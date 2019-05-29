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

import static org.corant.kernel.util.Preconditions.requireNotNull;
import static org.corant.shared.util.MapUtils.getMapLong;
import static org.corant.shared.util.MapUtils.getMapString;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractNamedReference extends AbstractReference implements Nameable {

  private static final long serialVersionUID = -8160589662074054451L;

  @Column(name = "referenceName")
  private String name;

  public AbstractNamedReference(Long id, Long vn, String name) {
    super(id, vn);
    setName(name);
  }

  public AbstractNamedReference(Object obj) {
    if (obj instanceof Map) {
      Map<?, ?> mapObj = requireNotNull(Map.class.cast(obj), "");// FIXME MSG
      setId(getMapLong(mapObj, "id"));
      setVn(getMapLong(mapObj, "vn"));
      setName(getMapString(mapObj, "name"));
    } else if (obj instanceof AbstractNamedReference) {
      AbstractNamedReference other = AbstractNamedReference.class.cast(obj);
      setId(other.getId());
      setVn(other.getVn());
      setName(other.getName());
    }
  }

  protected AbstractNamedReference() {}

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
    AbstractNamedReference other = (AbstractNamedReference) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  protected void setName(String name) {
    this.name = name;
  }

}
