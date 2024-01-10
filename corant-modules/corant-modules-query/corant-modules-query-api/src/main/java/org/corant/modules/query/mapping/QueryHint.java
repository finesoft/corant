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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.immutableList;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * corant-modules-query-api
 * <p>
 * The class defines query hints, which are used to provide additional programmable query
 * processing, such as query result processing, etc.
 * <p>
 * Generally it used with SPI interfaces. The query hints object may contain several parameters, a
 * script or expression. Each query hint must have a unique key which used to locate an
 * implementation of the SPI interface, Usually, the implementation of the SPI interface has a
 * method to detect whether a certain query hint is supported, and the key judgment is through the
 * {@link #key} of the query hint. Through this mechanism one can customize a query hint and an
 * implementation, the implementation will be invoked in query execution.
 *
 * @author bingo 下午7:35:54
 */
public class QueryHint implements Serializable {

  private static final long serialVersionUID = 50753651544743202L;

  private String key;
  private Map<String, List<QueryHintParameter>> parameters = new LinkedHashMap<>();
  private Script script = new Script();
  private final String id = UUID.randomUUID().toString();

  public QueryHint() {}

  /**
   * @param key unique key, used in conjunction with SPI-related interfaces to locate an
   *        implementation of the SPI interface
   * @param parameters the parameters used in the implementation of the SPI interface
   * @param script additional script or expression
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
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    QueryHint other = (QueryHint) obj;
    if (id == null) {
      return other.id == null;
    } else {
      return id.equals(other.id);
    }
  }

  /**
   * Returns the identifier of this query hint.
   */
  public String getId() {
    return id;
  }

  /**
   * A unique key, used in conjunction with SPI-related interfaces to locate an implementation of
   * the SPI interface.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns all pre-defined parameters used in the implementation of the SPI interface.
   */
  public Map<String, List<QueryHintParameter>> getParameters() {
    return parameters;
  }

  /**
   * Returns a pre-defined parameters list used in the implementation of the SPI interface by the
   * given name.
   */
  public List<QueryHintParameter> getParameters(String name) {
    return parameters.getOrDefault(name, emptyList());
  }

  /**
   * Returns an additional script used in the implementation of the SPI interface.
   */
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
    Map<String, List<QueryHintParameter>> temp = new LinkedHashMap<>();
    if (parameters != null) {
      parameters.forEach((k, v) -> temp.put(k, immutableList(v)));
    }
    parameters = unmodifiableMap(temp);
  }

  protected void setKey(String key) {
    this.key = key;
  }

  protected void setScript(Script script) {
    this.script = defaultObject(script, Script.EMPTY);
  }

  /**
   * corant-modules-query-api
   * <p>
   * Class define a pre-define query hint parameter, include parameter name, parameter value and
   * parameter type.
   *
   * @author bingo 上午11:44:54
   *
   */
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
     * Returns the name of this parameter
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the type of this parameter
     */
    public Class<?> getType() {
      return type;
    }

    /**
     * Returns the value of this parameter
     */
    public String getValue() {
      return value;
    }

    protected void setName(String name) {
      this.name = shouldNotNull(name);
    }

    protected void setType(Class<?> type) {
      this.type = defaultObject(type, Object.class);
    }

    protected void setValue(String value) {
      this.value = value;
    }

  }
}
