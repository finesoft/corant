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

import static org.corant.shared.util.MapUtils.getMapLong;
import static org.corant.shared.util.MapUtils.getMapString;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

/**
 * @author bingo 上午10:11:36
 *
 */
@Embeddable
@MappedSuperclass
public class Participator extends AbstractValueObject {

  private static final long serialVersionUID = -7820136962102596705L;

  public static final String CURRENT_USER_KEY = "_currentUser";
  public static final String CURRENT_ORG_KEY = "_currentOrg";

  static final Participator EMPTY_INST = new Participator();

  @Column(name = "participatorId")
  private Long id;

  @Column(name = "participatorName")
  private String name;

  public Participator(Long id, String name) {
    super();
    setId(id);
    setName(name);
  }

  protected Participator() {}

  public static Participator currentOrg() {
    return new Participator(-2L, "fake org");
  }

  public static Participator currentUser() {
    return new Participator(-1L, "fake user");
  }

  public static Participator empty() {
    return EMPTY_INST;
  }

  public static Participator of(Object obj) {
    if (obj instanceof Map) {
      Map<?, ?> mapObj = Map.class.cast(obj);
      return new Participator(getMapLong(mapObj, "id"), getMapString(mapObj, "name"));
    } else if (obj instanceof Party) {
      Party party = Party.class.cast(obj);
      return new Participator(party.getId(), party.getName());
    } else if (obj instanceof Participator) {
      Participator other = Participator.class.cast(obj);
      return new Participator(other.getId(), other.getName());
    }
    return null;
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
    Participator other = (Participator) obj;
    if (getId() == null) {
      if (other.getId() != null) {
        return false;
      }
    } else if (!getId().equals(other.getId())) {
      return false;
    }
    if (getName() == null) {
      if (other.getName() != null) {
        return false;
      }
    } else if (!getName().equals(other.getName())) {
      return false;
    }
    return true;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (getId() == null ? 0 : getId().hashCode());
    result = prime * result + (getName() == null ? 0 : getName().hashCode());
    return result;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  protected void setName(String name) {
    this.name = name;
  }


}
