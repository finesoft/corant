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
package org.corant.suites.query.shared.mapping;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
  private Script script = new Script();
  private final String id = UUID.randomUUID().toString();

  public QueryHint() {
    super();
  }

  /**
   * @param key
   * @param parameters
   * @param script
   */
  public QueryHint(String key, Map<String, List<QueryHintParameter>> parameters, Script script) {
    super();
    this.key = key;
    this.parameters = parameters;
    if (script != null) {
      this.script = script;
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

  protected void addParameter(QueryHintParameter parameter) {
    parameters.computeIfAbsent(parameter.getName(), (n) -> new ArrayList<>()).add(parameter);
  }

  protected void setKey(String key) {
    this.key = key;
  }

  protected void setScript(Script script) {
    this.script = defaultObject(script, Script.EMPTY);
  }

  /**
   * Make query immutable
   */
  void immunize() {
    parameters =
        parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(parameters);
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

    /**
     *
     * @param name the name to set
     */
    protected void setName(String name) {
      this.name = name;
    }

    /**
     *
     * @param type the type to set
     */
    protected void setType(String type) {
      this.type = type;
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
