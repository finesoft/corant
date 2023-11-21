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

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.UNDERSCORE;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * corant-modules-query-api
 * <p>
 * Query class, used to define a query. It contains the definition of query name version, query
 * script, sub-fetch query, query result set and parameters processing, etc.
 * <p>
 * Each query has a name and version, the name and version of the query form the identifier of the
 * query. The query script may be a SQL query script or NoSQL query script or a mixed script and
 * expression.
 *
 * @author bingo 上午10:22:33
 *
 */
public class Query implements Serializable {

  protected static final long serialVersionUID = -2142303696673387541L;

  private String name;
  private Class<?> resultClass = Map.class;
  private Class<?> resultSetMapping;
  private boolean cache = false;
  private boolean cacheResultSetMetadata = false;
  private String description;
  private Script script = new Script();
  private List<FetchQuery> fetchQueries = new ArrayList<>();
  private List<QueryHint> hints = new ArrayList<>();
  private String version = EMPTY;
  private Map<String, ParameterMapping> paramMappings = new HashMap<>();
  private Map<String, Class<?>> paramConvertSchema = new HashMap<>();
  private Map<String, String> properties = new HashMap<>();
  private String mappingFilePath;
  private String macroScript;// FIXME temporary
  private QueryType type; // since 2023-11-21
  private String qualifier; // since 2023-11-21

  public Query() {}

  public Query(String mappingFilePath) {
    this();
    setMappingFilePath(mappingFilePath);
  }

  /**
   * @param name the query name
   * @param resultClass the class of the query result records
   * @param resultSetMapping reserved field, may be used in the future
   * @param cache reserved field, may be used in the future
   * @param cacheResultSetMetadata reserved field, may be used in the future
   * @param description the query description
   * @param script the query script
   * @param fetchQueries the sub fetch query
   * @param hints the query hints use to adjust the query results
   * @param version the query version, The name and version of the query form the identifier of the
   *        query
   * @param paramMappings the query parameter mappings use for query parameter type conversion
   * @param properties the query execution properties, used to tune the execution of the query
   * @param mappingFilePath the file containing this query
   * @param macroScript the macro script use in this query
   */
  public Query(String name, Class<?> resultClass, Class<?> resultSetMapping, boolean cache,
      boolean cacheResultSetMetadata, String description, Script script,
      List<FetchQuery> fetchQueries, List<QueryHint> hints, String version,
      Map<String, ParameterMapping> paramMappings, Map<String, String> properties,
      String mappingFilePath, String macroScript) {
    setName(name);
    setResultClass(resultClass);
    setResultSetMapping(resultSetMapping);
    setCache(cache);
    setCacheResultSetMetadata(cacheResultSetMetadata);
    setDescription(description);
    if (script != null) {
      setScript(script);
    }
    if (fetchQueries != null) {
      this.fetchQueries.addAll(fetchQueries);
    }
    if (hints != null) {
      this.hints.addAll(hints);
    }
    setVersion(version);
    setParamMappings(paramMappings);
    setProperties(properties);
    setMappingFilePath(defaultStrip(mappingFilePath));
    setMacroScript(macroScript);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    Query other = (Query) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (version == null) {
      return other.version == null;
    } else {
      return version.equals(other.version);
    }
  }

  /**
   * Returns the query description
   *
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns all sub-fetch queries of this query
   */
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  /**
   * Returns all the query hints for this query.
   *
   * @see QueryHint
   *
   */
  public List<QueryHint> getHints() {
    return hints;
  }

  /**
   * Returns the macro for this query, for query scripts or expressions that support macros
   */
  public String getMacroScript() {
    return macroScript;
  }

  /**
   * Returns the mapping file path where this query come from
   */
  public String getMappingFilePath() {
    return mappingFilePath;
  }

  /**
   * Returns the query name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the query parameter conversion schema mapping, the key of the map is the name of the
   * query parameter and the value of the map is the type of the query parameter.
   */
  public Map<String, Class<?>> getParamConvertSchema() {
    return paramConvertSchema;
  }

  /**
   * Returns the source of the conversion schema
   */
  public Map<String, ParameterMapping> getParamMappings() {
    return paramMappings;
  }

  /**
   * Returns all query properties of this query. The properties can be used for some query process
   * control, such as timeout or maximum number of result sets, etc.
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Returns the value of query property by given name.
   */
  public Object getProperty(String name) {
    return getProperties() == null ? null : getProperties().get(name);
  }

  /**
   * Returns the property value of the specified type
   *
   * @param <T> property type
   * @param name property name
   * @param cls property type class
   * @return the property value
   */
  public <T> T getProperty(String name, Class<T> cls) {
    return toObject(getProperty(name), cls);
  }

  /**
   * Returns the property value of the specified type, if not found or be found is null return
   * alternative value.
   *
   * @param <T> property type
   * @param name property name
   * @param cls property type class
   * @param altVal if not found or be found is null return this value
   * @return the property value
   */
  public <T> T getProperty(String name, Class<T> cls, T altVal) {
    return defaultObject(getProperty(name, cls), altVal);
  }

  public String getQualifier() {
    return qualifier;
  }

  /**
   * Return the result class, if not setting return java.util.Map.class
   */
  public Class<?> getResultClass() {
    return resultClass;
  }

  /**
   * reserved field
   */
  public Class<?> getResultSetMapping() {
    return resultSetMapping;
  }

  /**
   * Returns the query script, the query script may be a SQL query script or NoSQL query script or a
   * mixed script and expression.
   */
  public Script getScript() {
    return script;
  }

  public QueryType getType() {
    return type;
  }

  /**
   * Returns the query version.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns the all sub-fetch query identifiers
   */
  public List<String> getVersionedFetchQueryNames() {
    return fetchQueries.stream().map(f -> f.getReferenceQuery().getVersionedName())
        .collect(Collectors.toList());
  }

  /**
   * Returns the query name and version, the name and version of the query form the identifier of
   * the query.
   */
  public String getVersionedName() {
    return defaultString(getName())
        + (isNotBlank(getVersion()) ? UNDERSCORE + getVersion() : EMPTY);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return prime * result + (version == null ? 0 : version.hashCode());
  }

  /**
   * reserved field
   */
  public boolean isCache() {
    return cache;
  }

  /**
   * reserved field
   */
  public boolean isCacheResultSetMetadata() {
    return cacheResultSetMetadata;
  }

  protected void addFetchQuery(FetchQuery fetchQuery) {
    fetchQueries.add(fetchQuery);
  }

  protected void addHint(QueryHint hint) {
    hints.add(hint);
  }

  protected void addProperty(String name, String value) {
    getProperties().put(name, value);
  }

  /**
   * Make query immutable
   */
  protected void postConstruct() {
    if (fetchQueries != null) {
      fetchQueries.forEach(FetchQuery::postConstruct);
      fetchQueries = Collections.unmodifiableList(fetchQueries);
    } else {
      fetchQueries = Collections.emptyList();
    }
    if (hints != null) {
      hints.forEach(QueryHint::postConstruct);
      hints = Collections.unmodifiableList(hints);
    } else {
      hints = Collections.emptyList();
    }
    setParamMappings(getParamMappings() == null ? Collections.emptyMap()
        : Collections.unmodifiableMap(getParamMappings()));
    setProperties(getProperties() == null ? Collections.emptyMap()
        : Collections.unmodifiableMap(getProperties()));

  }

  protected void setCache(boolean cache) {
    this.cache = cache;
  }

  protected void setCacheResultSetMetadata(boolean cacheResultSetMetadata) {
    this.cacheResultSetMetadata = cacheResultSetMetadata;
  }

  protected void setDescription(String description) {
    this.description = description;
  }

  protected void setMacroScript(String macroScript) {
    this.macroScript = macroScript;
  }

  protected void setMappingFilePath(String mappingFilePath) {
    this.mappingFilePath = mappingFilePath;
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setParamMappings(Map<String, ParameterMapping> paramMappings) {
    this.paramMappings.putAll(paramMappings);
    paramConvertSchema = Collections.unmodifiableMap(paramMappings.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getType())));
  }

  protected void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  protected void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  protected void setResultClass(Class<?> resultClass) {
    this.resultClass = resultClass;
  }

  protected void setResultSetMapping(Class<?> resultSetMapping) {
    this.resultSetMapping = resultSetMapping;
  }

  protected void setScript(Script script) {
    this.script = defaultObject(script, Script.EMPTY);
  }

  protected void setType(QueryType type) {
    this.type = type;
  }

  protected void setVersion(String version) {
    this.version = version;
  }

  /**
   * corant-modules-query-api
   * <p>
   * Query type, used to indicate the type of query script supported by the query, and also implies
   * the type of data system.
   *
   * @author bingo 上午11:23:59
   *
   */
  public enum QueryType {
    /**
     * Indicates that the query is a relational database query, such as MYSQL/MSSQL/ORACLE
     */
    SQL,
    /**
     * Indicates that the query is a Mongodb query.
     */
    MG,
    /**
     * Indicates that the query is a JPA query.
     */
    JPQL,
    /**
     * Indicates that the query is an elastic search query.
     */
    ES,
    /**
     * Indicates that the query is a cassandra query.
     */
    CAS
  }
}
