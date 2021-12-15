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
package org.corant.modules.query.mapping;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * corant-modules-query-api
 *
 * @author bingo 上午11:28:57
 *
 */
public class Properties {

  private final Set<Property> entries = new LinkedHashSet<>();

  public Map<String, String> toMap() {
    Map<String, String> map = new LinkedHashMap<>();
    entries.forEach(p -> map.put(p.name, p.value));
    return map;
  }

  protected void add(Property p) {
    entries.add(p);
  }

  protected void removeIf(Predicate<Property> p) {
    entries.removeIf(p);
  }

  public static class Property {

    private String name;
    private String value;

    /**
     * @param name
     * @param value
     */
    public Property(String name, String value) {
      this.name = name;
      this.value = value;
    }

    protected Property() {}

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Property other = (Property) obj;
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      if (value == null) {
        return other.value == null;
      } else {
        return value.equals(other.value);
      }
    }

    /**
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (name == null ? 0 : name.hashCode());
      return prime * result + (value == null ? 0 : value.hashCode());
    }

    /**
     *
     * @param name the name to set
     */
    protected void setName(String name) {
      this.name = name;
    }

    /**
     *
     * @param value the value to set
     */
    protected void setValue(String value) {
      this.value = value;
    }

  }
}
