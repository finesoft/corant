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
public abstract class AbstractPPTReference extends AbstractReference implements Nameable, Numbered {

  private static final long serialVersionUID = -8160589662074054451L;

  @Column(name = "referenceName")
  private String name;

  @Column(name = "referenceNumber")
  private String number;

  public AbstractPPTReference(Long id, long vn, String name, String number) {
    setId(id);
    setVn(vn);
    setName(name);
    setNumber(number);
  }

  public AbstractPPTReference(Object obj) {
    if (obj instanceof Map) {
      Map<?, ?> mapObj = requireNotNull(Map.class.cast(obj), "");// FIXME MSG
      setId(getMapLong(mapObj, "id"));
      setVn(getMapLong(mapObj, "vn"));
      setName(getMapString(mapObj, "name"));
      setNumber(getMapString(mapObj, "number"));
    } else if (obj instanceof AbstractPPTReference) {
      AbstractPPTReference other = AbstractPPTReference.class.cast(obj);
      setId(other.getId());
      setVn(other.getVn());
      setName(other.getName());
      setNumber(other.getNumber());
    }
  }

  protected AbstractPPTReference() {}

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getNumber() {
    return number;
  }

  @Override
  public String toString() {
    return "AbstractPPTReference [name=" + name + ", number=" + number + ", getId()=" + getId()
        + ", getVn()=" + getVn() + "]";
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setNumber(String number) {
    this.number = number;
  }

}
