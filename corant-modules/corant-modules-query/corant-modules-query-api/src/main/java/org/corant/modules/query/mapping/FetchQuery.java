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

import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Strings;

/**
 * corant-modules-query-api
 * <p>
 * Fetch query, mainly used to perform sub-queries based on the parent query result set and
 * parameters, and inject the results into the result set of the parent query. The fetch query is
 * generally used to query complex compound object result sets.
 * <p>
 * Fetch query actually defines a sub-query invocation, so each fetch query must refer to a real
 * {@link Query} and define the parameter and result set type of this query, and the processing
 * method of injecting into the parent query result, etc.
 *
 * @author bingo 上午10:26:45
 */
public class FetchQuery implements Serializable {

  private static final long serialVersionUID = 449192431797295206L;
  private QueryReference referenceQuery = new QueryReference();
  private String injectPropertyName;
  private String[] injectPropertyNamePath = Strings.EMPTY_ARRAY;
  private Class<?> resultClass = Map.class;
  private int maxSize = -1;
  private List<FetchQueryParameter> parameters = new ArrayList<>();
  private boolean multiRecords = true;
  private Script predicateScript = new Script();
  private Script injectionScript = new Script();
  private boolean eagerInject = true;
  private final String id = UUID.randomUUID().toString();

  public FetchQuery() {}

  /**
   * Construct a fetch query
   *
   * @param referenceQuery the real query corresponding to this fetch query
   * @param injectPropertyName the name of the parent query result property which use to hold the
   *        fetch query results
   * @param resultClass the fetch query result record class
   * @param maxSize the max fetch query result size, -1 means unlimited
   * @param parameters the fetch query parameters
   * @param multiRecords indicates that the fetch query result set is multiple records
   * @param predicate the script use to detect whether performance the fetch query
   * @param injection the script use to performance inject the fetch query result to parent query
   *        results.
   * @param eagerInject indicates whether to performance the fetch query eager.
   */
  public FetchQuery(QueryReference referenceQuery, String injectPropertyName, Class<?> resultClass,
      int maxSize, List<FetchQueryParameter> parameters, boolean multiRecords, Script predicate,
      Script injection, boolean eagerInject) {
    setReferenceQuery(referenceQuery);
    setInjectPropertyName(injectPropertyName);
    setResultClass(resultClass);
    setMaxSize(maxSize);
    if (parameters != null) {
      this.parameters.addAll(parameters);
    }
    setMultiRecords(multiRecords);
    if (predicate != null) {
      setPredicateScript(predicate);
    }
    if (injection != null) {
      setInjectionScript(injection);
    }
    setEagerInject(eagerInject);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FetchQuery other = (FetchQuery) obj;
    if (id == null) {
      return other.id == null;
    } else {
      return id.equals(other.id);
    }
  }

  /**
   * Returns the fetch query identifier, each fetch query has a unique identifier.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the script use to performance inject the fetch query result to parent query results.
   */
  public Script getInjectionScript() {
    return injectionScript;
  }

  /**
   * Returns the name of the parent query result property which use to hold the fetch query results
   * <p>
   * If there is a hierarchical result, use '.' as the hierarchical separator for the property path.
   */
  public String getInjectPropertyName() {
    return injectPropertyName;
  }

  /**
   * Returns the hierarchical property names.
   */
  public String[] getInjectPropertyNamePath() {
    return injectPropertyNamePath;
  }

  /**
   * Returns the max fetch query result size, -1 means unlimited
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * Returns the fetch query parameters.
   */
  public List<FetchQueryParameter> getParameters() {
    return parameters;
  }

  /**
   * Returns the script use to detect whether performance the fetch query
   */
  public Script getPredicateScript() {
    return predicateScript;
  }

  /**
   * Returns the real query corresponding to this fetch query
   */
  public QueryReference getReferenceQuery() {
    return referenceQuery;
  }

  /**
   * Returns the class of the fetch query result record
   */
  public Class<?> getResultClass() {
    return resultClass;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (id == null ? 0 : id.hashCode());
  }

  /**
   * Return whether to performance the fetch query eager.
   */
  public boolean isEagerInject() {
    return eagerInject;
  }

  /**
   * Returns true if the fetch query result set is multiple records, otherwise false.
   */
  public boolean isMultiRecords() {
    return multiRecords;
  }

  protected void addParameter(FetchQueryParameter parameter) {
    parameters.add(parameter);
  }

  /**
   * Make query immutable
   */
  protected void postConstruct() {
    parameters =
        parameters == null ? Collections.emptyList() : Collections.unmodifiableList(parameters);
  }

  protected void setEagerInject(boolean eagerInject) {
    this.eagerInject = eagerInject;
  }

  protected void setInjectionScript(Script injection) {
    injectionScript = defaultObject(injection, Script.EMPTY);
  }

  protected void setInjectPropertyName(String injectPropertyName) {
    this.injectPropertyName = injectPropertyName;
    // injectPropertyNamePath = split(injectPropertyName, Names.NAME_SPACE_SEPARATORS, true, false);
    injectPropertyNamePath = Names.splitNameSpace(injectPropertyName, true, false);
  }

  protected void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  protected void setMultiRecords(boolean multiRecords) {
    this.multiRecords = multiRecords;
  }

  protected void setPredicateScript(Script predicate) {
    predicateScript = defaultObject(predicate, Script.EMPTY);
  }

  protected void setReferenceQuery(QueryReference referenceQuery) {
    this.referenceQuery = referenceQuery;
  }

  protected void setReferenceQueryName(String referenceQueryName) {
    referenceQuery.setName(referenceQueryName);
  }

  protected void setReferenceQueryQualifier(String referenceQueryQualifier) {
    referenceQuery.setQualifier(referenceQueryQualifier);
  }

  protected void setReferenceQueryType(QueryType referenceQueryType) {
    referenceQuery.setType(referenceQueryType);
  }

  protected void setReferenceQueryVersion(String referenceQueryVersion) {
    referenceQuery.setVersion(referenceQueryVersion);
  }

  protected void setResultClass(Class<?> resultClass) {
    this.resultClass = defaultObject(resultClass, Map.class);
  }

  /**
   * corant-modules-query-api
   * <p>
   * Fetch query parameter, which defines the query parameters used in the fetch query. Including
   * parameter name, value source, value type, etc.
   *
   * @see FetchQueryParameterSource
   *
   * @author bingo 上午11:14:41
   *
   */
  public static class FetchQueryParameter implements Serializable {

    private static final long serialVersionUID = 5013658267151165784L;

    private String name;
    private String sourceName;
    private String[] sourceNamePath = Strings.EMPTY_ARRAY;
    private FetchQueryParameterSource source;
    private String value;
    private Class<?> type;
    private boolean distinct = true;
    private boolean singleAsList = false;
    private boolean flatten = true;
    private Script script;
    private String group;

    public FetchQueryParameter() {}

    /**
     * @param group the parameter group use for aggregate parameters to a collection
     * @param name the parameter name
     * @param sourceName the source name used with source
     * @param source indicates the value source of the fetch query parameter
     * @param value the parameter value, usually used for the source is the specified constant
     * @param type the target type of the parameter value, usually the parameter value will undergo
     *        type conversion
     * @param script a script function used to calculate parameter values, usually for parameters
     *        whose source is a script, where the parameters of the script function are parent query
     *        parameters and parent query result sets, and the return value of the script is the
     *        fetch query parameter value
     * @param distinct whether to de-duplicate when there are multiple parameter values
     * @param singleAsList when the value is a single value, whether to convert it to a list
     * @param flatten When the parameter value is multiple collections, whether to extract the
     *        values in the collection to form a new collection
     */
    public FetchQueryParameter(String group, String name, String sourceName,
        FetchQueryParameterSource source, String value, Class<?> type, Script script,
        boolean distinct, boolean singleAsList, boolean flatten) {
      setGroup(group);
      setName(name);
      setSourceName(sourceName);
      setSource(source);
      setValue(value);
      setType(type);
      setScript(script);
      setDistinct(distinct);
      setSingleAsList(singleAsList);
      setFlatten(flatten);
    }

    public String getGroup() {
      return group;
    }

    /**
     * Returns the fetch query parameter name
     */
    public String getName() {
      return name;
    }

    /**
     * Returns a script function used to calculate parameter values, usually for parameters whose
     * source is a script, where the parameters of the script function are parent query parameters
     * and parent query result sets, and the return value of the script is the fetch query parameter
     * value
     */
    public Script getScript() {
      return script;
    }

    /**
     * Returns the value source of the fetch query parameter
     */
    public FetchQueryParameterSource getSource() {
      return source;
    }

    /**
     * Returns the source name used with source
     */
    public String getSourceName() {
      return sourceName;
    }

    public String[] getSourceNamePath() {
      return sourceNamePath;
    }

    /**
     * Returns the target type of the parameter value, usually the parameter value will undergo type
     * conversion
     */
    public Class<?> getType() {
      return type;
    }

    /**
     * Returns the parameter value, usually used for the source is the specified constant. In
     * general if the source is not a constant this method should return null.
     */
    public String getValue() {
      return value;
    }

    /**
     * Returns whether to de-duplicate when there are multiple parameter values
     */
    public boolean isDistinct() {
      return distinct;
    }

    public boolean isFlatten() {
      return flatten;
    }

    /**
     * Returns whether to convert the value to a list when the value is a single value.
     */
    public boolean isSingleAsList() {
      return singleAsList;
    }

    protected void setDistinct(boolean distinct) {
      this.distinct = distinct;
    }

    protected void setFlatten(boolean flatten) {
      this.flatten = flatten;
    }

    protected void setGroup(String group) {
      this.group = group;
    }

    protected void setName(String name) {
      this.name = name;
    }

    protected void setScript(Script script) {
      this.script = script;
    }

    protected void setSingleAsList(boolean singleAsList) {
      this.singleAsList = singleAsList;
    }

    protected void setSource(FetchQueryParameterSource source) {
      this.source = source;
    }

    protected void setSourceName(String sourceName) {
      this.sourceName = sourceName;
      // sourceNamePath = split(sourceName, Names.NAME_SPACE_SEPARATORS, true, false);
      sourceNamePath = Names.splitNameSpace(sourceName, true, false);
    }

    protected void setType(Class<?> type) {
      this.type = type;
    }

    protected void setValue(String value) {
      this.value = value;
    }

  }

  /**
   * corant-modules-query-api
   * <p>
   * Defines the source of several fetch query parameters.
   *
   * @author bingo 上午11:21:02
   *
   */
  public enum FetchQueryParameterSource {
    /**
     * Indicates that the value of the fetch query parameter is extracted from parent query
     * parameters.
     */
    P,
    /**
     * Indicates that the value of the fetch query parameter is extracted from parent query results.
     */
    R,
    /**
     * Indicates that the value of the fetch query parameter is a constant.
     */
    C,
    /**
     * Indicates that the value of the fetch query parameter is the execution result of the
     * specified script function, where the parameters of the script function are the parent query
     * parameters and the query result set. This is an experimental feature.
     */
    S
  }

}
