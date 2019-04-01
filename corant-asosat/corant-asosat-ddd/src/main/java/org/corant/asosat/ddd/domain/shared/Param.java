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

import static org.corant.shared.util.MapUtils.asMap;
import java.io.Serializable;
import java.util.Map;
import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;

/**
 * @author bingo 下午2:02:31
 *
 */
public class Param implements Serializable {

  public static final String AT_CFM_STATUS_KEY = "confirmationStatus";

  static final Param EMPTY_INST = new Param();

  private static final long serialVersionUID = -3517537674124343136L;

  private final DynamicAttributeMap attributes = new DynamicAttributeMap();

  private Participator operator = Participator.currentUser();

  protected Param() {}

  protected Param(DynamicAttributeMap attributes) {
    this(Participator.empty(), attributes);
  }

  protected Param(Participator operator) {
    this(operator, null);
  }

  protected Param(Participator operator, DynamicAttributeMap attributes) {
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
    this.operator = operator;
  }

  public static Param empty() {
    return EMPTY_INST;
  }

  public static Param of(DynamicAttributeMap attributes) {
    return new Param(attributes);
  }

  public static Param of(Participator operator) {
    return new Param(operator);
  }

  public static Param of(Participator operator, DynamicAttributeMap attributes) {
    return new Param(operator, attributes);
  }

  public Param clearAttributes(String name) {
    Param newer = new Param(operator, null);
    return newer;
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
    Param other = (Param) obj;
    if (attributes == null) {
      if (other.attributes != null) {
        return false;
      }
    } else if (!attributes.equals(other.attributes)) {
      return false;
    }
    if (operator == null) {
      if (other.operator != null) {
        return false;
      }
    } else if (!operator.equals(other.operator)) {
      return false;
    }
    return true;
  }

  public DynamicAttributeMap getAttributes() {
    return attributes.unmodifiable();
  }

  public Participator getOperator() {
    return operator == null ? Participator.empty() : operator;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (attributes == null ? 0 : attributes.hashCode());
    result = prime * result + (operator == null ? 0 : operator.hashCode());
    return result;
  }

  public Param removeAttribute(String name) {
    Param newer = new Param(operator, attributes);
    newer.attributes.remove(name);
    return newer;
  }

  public Param withAttribute(Map<?, ?> map) {
    Param newer = new Param(operator, attributes);
    map.forEach((k, v) -> {
      if (k != null) {
        newer.attributes.put(k.toString(), v);
      }
    });
    return newer;
  }

  public Param withAttribute(String name, Object value) {
    Param newer = new Param(operator, attributes);
    newer.attributes.put(name, value);
    return newer;
  }

  public Param withAttributeIfAbsent(String name, Object value) {
    Param newer = new Param(operator, attributes);
    newer.attributes.putIfAbsent(name, value);
    return newer;
  }

  public Param withAttributes(Object... attrs) {
    Param newer = new Param(operator, attributes);
    newer.attributes.putAll(asMap(attrs));
    return newer;
  }

}
