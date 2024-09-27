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
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * Fetch query actually defines a sub-query invocation, so each fetch query can refer to an existing
 * external query or define an in-line query but can't do either, as well as defining the parameter
 * and result set type of the query, and the processing method of injecting into the parent query
 * result, etc.
 * <p>
 * If fetch query define an in-line query, it can have owner sub-fetch query.
 *
 * @author bingo 上午10:26:45
 */
public class FetchQuery implements Serializable {

  private static final long serialVersionUID = 449192431797295206L;

  protected final String id = UUID.randomUUID().toString();

  protected QueryReference queryReference;

  protected String inlineQueryName; // since 2024-08-31
  protected QueryType inlineQueryType; // since 2024-08-31
  protected String inlineQueryQualifier; // since 2024-08-31
  protected boolean inlineQueryCache;// since 2024-08-31
  protected Map<String, String> properties = new HashMap<>(); // since 2024-08-31
  protected Script inlineQueryScript = Script.EMPTY; // since 2024-08-31

  protected String referenceQueryName;
  protected QueryType referenceQueryType;
  protected String referenceQueryQualifier;
  protected String referenceQueryVersion;

  protected String description;

  protected String injectPropertyName;
  protected String[] injectPropertyNamePath = Strings.EMPTY_ARRAY;
  protected Class<?> resultClass = Map.class;
  protected int maxSize = 0;
  protected List<FetchQueryParameter> parameters = new ArrayList<>();
  protected boolean multiRecords = true;
  protected Script predicateScript = new Script();
  protected Script injectionScript = new Script();
  protected boolean eagerInject = true;

  protected List<FetchQuery> fetchQueries = new ArrayList<>();// 2024-08-31

  public FetchQuery() {}

  /**
   * Construct an in-line fetch query
   *
   * @param inlineQueryName the in-line fetch query name
   * @param inlineQueryType the in-line fetch query type
   * @param inlineQueryQualifier the in-line fetch query qualifier
   * @param properties the query execution properties, used to tune the execution of the query
   * @param inlineQueryScript the in-line fetch query script
   * @param inlineQueryCache whether to cache in-line query result
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
   * @param fetchQueries the sub-fetch query
   */
  public FetchQuery(String inlineQueryName, QueryType inlineQueryType, String inlineQueryQualifier,
      Map<String, String> properties, Script inlineQueryScript, boolean inlineQueryCache,
      String injectPropertyName, Class<?> resultClass, int maxSize,
      List<FetchQueryParameter> parameters, boolean multiRecords, Script predicate,
      Script injection, boolean eagerInject, List<FetchQuery> fetchQueries) {
    setInlineQueryName(inlineQueryName);
    setInlineQueryType(inlineQueryType);
    setInlineQueryQualifier(inlineQueryQualifier);
    setProperties(properties);
    setInlineQueryCache(inlineQueryCache);
    setInlineQueryScript(inlineQueryScript);
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
    if (isNotEmpty(fetchQueries)) {
      for (FetchQuery fq : fetchQueries) {
        addFetchQuery(fq);
      }
    }
  }

  /**
   * Construct a reference fetch query
   *
   * @param referenceQueryName the real query name corresponding to this fetch query
   * @param referenceQueryType the real query type corresponding to this fetch query
   * @param referenceQueryQualifier the real query qualifier corresponding to this fetch query
   * @param referenceQueryVersion the real query version corresponding to this fetch query
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
  public FetchQuery(String referenceQueryName, QueryType referenceQueryType,
      String referenceQueryQualifier, String referenceQueryVersion, String injectPropertyName,
      Class<?> resultClass, int maxSize, List<FetchQueryParameter> parameters, boolean multiRecords,
      Script predicate, Script injection, boolean eagerInject) {
    setReferenceQueryName(referenceQueryName);
    setReferenceQueryType(referenceQueryType);
    setReferenceQueryQualifier(referenceQueryQualifier);
    setReferenceQueryVersion(referenceQueryVersion);
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

  public String getDescription() {
    return description;
  }

  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
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
    return Arrays.copyOf(injectPropertyNamePath, injectPropertyNamePath.length);
  }

  /**
   * Returns the in-line query name
   */
  public String getInlineQueryName() {
    return inlineQueryName;
  }

  /**
   * Returns the in-line query qualifier
   */
  public String getInlineQueryQualifier() {
    return inlineQueryQualifier;
  }

  /**
   * Returns the in-line query script
   */
  public Script getInlineQueryScript() {
    return inlineQueryScript;
  }

  /**
   * Returns the in-line query type
   */
  public QueryType getInlineQueryType() {
    return inlineQueryType;
  }

  /**
   * Returns the max fetch query result size, 0 means unlimited
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
   * Returns all in-line query properties of this query. The properties can be used for some query
   * process control, such as timeout or maximum number of result sets, etc.
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  public QueryReference getQueryReference() {
    return queryReference;
  }

  public String getReferenceQueryName() {
    return referenceQueryName;
  }

  public String getReferenceQueryQualifier() {
    return referenceQueryQualifier;
  }

  public QueryType getReferenceQueryType() {
    return referenceQueryType;
  }

  public String getReferenceQueryVersion() {
    return referenceQueryVersion;
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

  public boolean isInlineQueryCache() {
    return inlineQueryCache;
  }

  /**
   * Returns true if the fetch query result set is multiple records, otherwise false.
   */
  public boolean isMultiRecords() {
    return multiRecords;
  }

  protected void addFetchQuery(FetchQuery fetchQuery) {
    fetchQueries.add(fetchQuery);
  }

  protected void addParameter(FetchQueryParameter parameter) {
    parameters.add(parameter);
  }

  protected void addProperty(String name, String value) {
    getProperties().put(name, value);
  }

  /**
   * Validate and make immutable
   */
  protected void postConstruct() {
    parameters = parameters == null ? emptyList() : unmodifiableList(parameters);
    fetchQueries = fetchQueries == null ? emptyList() : unmodifiableList(fetchQueries);
    properties = properties == null ? emptyMap() : unmodifiableMap(properties);
  }

  protected void setDescription(String description) {
    this.description = description;
  }

  protected void setEagerInject(boolean eagerInject) {
    this.eagerInject = eagerInject;
  }

  protected void setInjectionScript(Script injection) {
    injectionScript = defaultObject(injection, Script.EMPTY);
  }

  protected void setInjectPropertyName(String injectPropertyName) {
    this.injectPropertyName = injectPropertyName;
    injectPropertyNamePath = Names.splitNameSpace(injectPropertyName, true, false);
  }

  protected void setInlineQueryCache(boolean inlineQueryCache) {
    this.inlineQueryCache = inlineQueryCache;
  }

  protected void setInlineQueryName(String inlineQueryName) {
    this.inlineQueryName = inlineQueryName;
  }

  protected void setInlineQueryQualifier(String inlineQueryQualifier) {
    this.inlineQueryQualifier = inlineQueryQualifier;
  }

  protected void setInlineQueryScript(Script inlineQueryScript) {
    this.inlineQueryScript = inlineQueryScript;
  }

  protected void setInlineQueryType(QueryType inlineQueryType) {
    this.inlineQueryType = inlineQueryType;
  }

  protected void setMaxSize(int maxSize) {
    this.maxSize = Math.max(0, maxSize);
  }

  protected void setMultiRecords(boolean multiRecords) {
    this.multiRecords = multiRecords;
  }

  protected void setPredicateScript(Script predicate) {
    predicateScript = defaultObject(predicate, Script.EMPTY);
  }

  protected void setProperties(Map<String, String> properties) {
    this.properties.clear();
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  protected void setQueryReference(QueryReference queryReference) {
    this.queryReference = queryReference;
  }

  protected void setReferenceQueryName(String referenceQueryName) {
    this.referenceQueryName = referenceQueryName;
  }

  protected void setReferenceQueryQualifier(String referenceQueryQualifier) {
    this.referenceQueryQualifier = referenceQueryQualifier;
  }

  protected void setReferenceQueryType(QueryType referenceQueryType) {
    this.referenceQueryType = referenceQueryType;
  }

  protected void setReferenceQueryVersion(String referenceQueryVersion) {
    this.referenceQueryVersion = referenceQueryVersion;
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

    protected String name;
    protected String sourceName;
    protected String[] sourceNamePath = Strings.EMPTY_ARRAY;
    protected FetchQueryParameterSource source;
    protected String value;
    protected Class<?> type;
    protected boolean distinct = true;
    protected boolean singleAsList = false;
    protected boolean flatten = true;
    protected Script script;
    protected String group;
    protected String[] groupPath = Strings.EMPTY_ARRAY;
    protected Nullable nullable = Nullable.AUTO;

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
     * @param nullable supports null value as parameter value
     */
    public FetchQueryParameter(String group, String name, String sourceName,
        FetchQueryParameterSource source, String value, Class<?> type, Script script,
        boolean distinct, boolean singleAsList, boolean flatten, Nullable nullable) {
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
      setNullable(nullable);
    }

    public String getGroup() {
      return group;
    }

    public String[] getGroupPath() {
      return Arrays.copyOf(groupPath, groupPath.length);
    }

    /**
     * Returns the fetch query parameter name
     */
    public String getName() {
      return name;
    }

    public Nullable getNullable() {
      return nullable;
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
      groupPath = Names.splitNameSpace(group, true, true);
    }

    protected void setName(String name) {
      this.name = name;
    }

    protected void setNullable(Nullable nullable) {
      this.nullable = defaultObject(nullable, Nullable.AUTO);
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
