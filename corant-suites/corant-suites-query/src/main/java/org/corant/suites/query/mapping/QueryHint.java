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
package org.corant.suites.query.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:35:54
 *
 */
public class QueryHint implements Serializable {

  private static final long serialVersionUID = 50753651544743202L;

  private String key;
  private Map<String, List<QueryHintParameter>> parameters = new LinkedHashMap<>();
  private String script;

  private volatile Integer hash = null;

  public QueryHint() {
    super();
  }

  /**
   * @param key
   * @param parameters
   * @param script
   * @param hash
   */
  public QueryHint(String key, Map<String, List<QueryHintParameter>> parameters, String script,
      Integer hash) {
    super();
    this.key = key;
    this.parameters = parameters;
    this.script = script;
    this.hash = hash;
  }

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
    QueryHint other = (QueryHint) obj;
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    if (parameters == null) {
      if (other.parameters != null) {
        return false;
      }
    } else if (!parameters.equals(other.parameters)) {
      return false;
    }
    if (script == null) {
      if (other.script != null) {
        return false;
      }
    } else if (!script.equals(other.script)) {
      return false;
    }
    return true;
  }

  public String getKey() {
    return key;
  }

  public Map<String, List<QueryHintParameter>> getParameters() {
    return parameters;
  }

  public List<QueryHintParameter> getParameters(String name) {
    if (parameters.containsKey(name)) {
      return parameters.get(name);
    } else {
      return new ArrayList<>();
    }
  }

  public String getScript() {
    return script;
  }

  @Override
  public int hashCode() {
    if (hash == null) {
      synchronized (this) {
        if (hash == null) {
          final int prime = 31;
          int result = 1;
          result = prime * result + (key == null ? 0 : key.hashCode());
          result = prime * result + (parameters == null ? 0 : parameters.hashCode());
          result = prime * result + (script == null ? 0 : script.hashCode());
          hash = Integer.valueOf(result);
        }
      }
    }
    return hash.intValue();
  }

  void addParameter(QueryHintParameter parameter) {
    parameters.computeIfAbsent(parameter.getName(), (n) -> new ArrayList<>()).add(parameter);
  }

  void setKey(String key) {
    this.key = key;
  }

  void setScript(String script) {
    this.script = script;
  }

  public static class QueryHintParameter implements Serializable {

    private static final long serialVersionUID = -875004740413444084L;

    private String name;
    private String value;
    private String type;

    public QueryHintParameter() {
      super();
    }

    /**
     * @param name
     * @param value
     * @param type
     */
    public QueryHintParameter(String name, String value, String type) {
      super();
      this.name = name;
      this.value = value;
      this.type = type;
    }

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
      QueryHintParameter other = (QueryHintParameter) obj;
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      return true;
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
     * @return the type
     */
    public String getType() {
      return type;
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
      return result;
    }

    /**
     *
     * @param name the name to set
     */
    void setName(String name) {
      this.name = name;
    }

    /**
     *
     * @param type the type to set
     */
    void setType(String type) {
      this.type = type;
    }

    /**
     *
     * @param value the value to set
     */
    void setValue(String value) {
      this.value = value;
    }

  }
}
