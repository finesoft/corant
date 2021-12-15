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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午7:35:54
 *
 */
public class QueryHint implements Serializable {

  private static final long serialVersionUID = 50753651544743202L;

  private String key;
  private Map<String, List<QueryHintParameter>> parameters = new LinkedHashMap<>();
  private Script script = new Script();
  private final String id = UUID.randomUUID().toString();

  public QueryHint() {}

  /**
   * @param key
   * @param parameters
   * @param script
   */
  public QueryHint(String key, Map<String, List<QueryHintParameter>> parameters, Script script) {
    this.key = key;
    this.parameters = parameters;
    if (script != null) {
      this.script = script;
    }
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
    if (id == null) {
      return other.id == null;
    } else {
      return id.equals(other.id);
    }
  }

  public String getId() {
    return id;
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

  public Script getScript() {
    return script;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (id == null ? 0 : id.hashCode());
  }

  protected void addParameter(QueryHintParameter parameter) {
    parameters.computeIfAbsent(parameter.getName(), n -> new ArrayList<>()).add(parameter);
  }

  /**
   * Make query immutable
   */
  protected void postConstruct() {
    parameters =
        parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(parameters);
  }

  protected void setKey(String key) {
    this.key = key;
  }

  protected void setScript(Script script) {
    this.script = defaultObject(script, Script.EMPTY);
  }

  public static class QueryHintParameter implements Serializable {

    private static final long serialVersionUID = -875004740413444084L;

    private String name;
    private String value;
    private Class<?> type = Object.class;

    public QueryHintParameter() {}

    public QueryHintParameter(String name, String value, Class<?> type) {
      setName(name);
      setValue(value);
      setType(type);
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
    public Class<?> getType() {
      return type;
    }

    /**
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     *
     * @param name the name to set
     */
    protected void setName(String name) {
      this.name = shouldNotNull(name);
    }

    /**
     *
     * @param type the type to set
     */
    protected void setType(Class<?> type) {
      this.type = defaultObject(type, Object.class);
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
