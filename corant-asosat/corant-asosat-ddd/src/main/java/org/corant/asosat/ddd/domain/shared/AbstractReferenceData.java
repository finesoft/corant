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
import java.io.Serializable;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

@MappedSuperclass
public abstract class AbstractReferenceData extends AbstractValueObject
    implements Nameable, Serializable {

  private static final long serialVersionUID = -8160589662074054451L;

  @Column(name = "referenceId")
  private Long id;

  @Column(name = "referenceVn")
  private long vn;

  @Column(name = "referenceName")
  private String name;

  public AbstractReferenceData(Long id, long vn, String name) {
    super();
    setId(id);
    setVn(vn);
    setName(name);
  }

  public AbstractReferenceData(Object obj) {
    if (obj instanceof Map) {
      Map<?, ?> mapObj = requireNotNull(Map.class.cast(obj), "");// FIXME MSG
      setId(getMapLong(mapObj, "id"));
      setVn(getMapLong(mapObj, "vn"));
      setName(getMapString(mapObj, "name"));
    } else if (obj instanceof AbstractReferenceData) {
      AbstractReferenceData other = AbstractReferenceData.class.cast(obj);
      setId(other.getId());
      setVn(other.getVn());
      setName(other.getName());
    }
  }

  protected AbstractReferenceData() {}

  public Long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public long getVn() {
    return vn;
  }

  @Override
  public String toString() {
    return "Industry [id=" + getId() + ", vn=" + getVn() + ", name=" + getName() + "]";
  }

  protected void setId(Long id) {
    this.id = id;
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setVn(long vn) {
    this.vn = vn;
  }

}
