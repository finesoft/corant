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

import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:22:33
 *
 */
public class Query implements Serializable {

  protected static final long serialVersionUID = -2142303696673387541L;

  private String name;
  private Class<?> resultClass = Map.class;
  private Class<?> resultSetMapping;
  private boolean cache = true;
  private boolean cacheResultSetMetadata = true;
  private String description;
  private String script;
  private List<FetchQuery> fetchQueries = new ArrayList<>();
  private List<QueryHint> hints = new ArrayList<>();
  private String version = "";
  private Map<String, ParameterMapping> paramMappings = new HashMap<>();
  private Map<String, String> properties = new HashMap<>();

  public Query() {
    super();
  }

  /**
   * @param name
   * @param resultClass
   * @param resultSetMapping
   * @param cache
   * @param cacheResultSetMetadata
   * @param description
   * @param script
   * @param fetchQueries
   * @param hints
   * @param version
   * @param paramMappings
   * @param properties
   */
  public Query(String name, Class<?> resultClass, Class<?> resultSetMapping, boolean cache,
      boolean cacheResultSetMetadata, String description, String script,
      List<FetchQuery> fetchQueries, List<QueryHint> hints, String version,
      Map<String, ParameterMapping> paramMappings, Map<String, String> properties) {
    super();
    this.name = name;
    setResultClass(resultClass);
    this.resultSetMapping = resultSetMapping;
    this.cache = cache;
    this.cacheResultSetMetadata = cacheResultSetMetadata;
    this.description = description;
    this.script = script;
    this.fetchQueries = fetchQueries;
    this.hints = hints;
    this.version = version;
    this.paramMappings = paramMappings;
    this.properties = properties;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the fetchQueries
   */
  public List<FetchQuery> getFetchQueries() {
    return Collections.unmodifiableList(fetchQueries);
  }

  /**
   * @return the hints
   */
  public List<QueryHint> getHints() {
    return Collections.unmodifiableList(hints);
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public Map<String, Class<?>> getParamConvertSchema() {
    return Collections.unmodifiableMap(paramMappings.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getType())));
  }

  /**
   * @return the paramMappings
   */
  public Map<String, ParameterMapping> getParamMappings() {
    return Collections.unmodifiableMap(paramMappings);
  }

  /**
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public <T> T getProperty(String name, Class<T> cls) {
    return properties == null ? null : toObject(properties.get(name), cls);
  }

  public <T> T getProperty(String name, Class<T> cls, T altVal) {
    return defaultObject(getProperty(name, cls), altVal);
  }

  /**
   * @return the resultClass
   */
  public Class<?> getResultClass() {
    return defaultObject(resultClass, Map.class);
  }

  /**
   * @return the resultSetMapping
   */
  public Class<?> getResultSetMapping() {
    return resultSetMapping;
  }

  /**
   * @return the script
   */
  public String getScript() {
    return script;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  public List<String> getVersionedFetchQueryNames() {
    return fetchQueries.stream().map(f -> f.getVersionedReferenceQueryName())
        .collect(Collectors.toList());
  }

  public String getVersionedName() {
    return defaultString(name) + (isNotBlank(version) ? "_" + version : "");
  }

  /**
   * @return the cache
   */
  public boolean isCache() {
    return cache;
  }

  /**
   * @return the cacheResultSetMetadata
   */
  public boolean isCacheResultSetMetadata() {
    return cacheResultSetMetadata;
  }

  void addFetchQuery(FetchQuery fetchQuery) {
    fetchQueries.add(fetchQuery);
  }

  void addHint(QueryHint hint) {
    hints.add(hint);
  }

  void addProperty(String name, String value) {
    properties.put(name, value);
  }

  void setCache(boolean cache) {
    this.cache = cache;
  }

  void setCacheResultSetMetadata(boolean cacheResultSetMetadata) {
    this.cacheResultSetMetadata = cacheResultSetMetadata;
  }

  void setDescription(String description) {
    this.description = description;
  }

  void setName(String name) {
    this.name = name;
  }

  void setParamMappings(Map<String, ParameterMapping> paramMappings) {
    this.paramMappings.putAll(paramMappings);
  }

  /**
   * @param properties the properties to set
   */
  void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  void setResultClass(Class<?> resultClass) {
    this.resultClass = defaultObject(resultClass, Map.class);
  }

  void setResultSetMapping(Class<?> resultSetMapping) {
    this.resultSetMapping = resultSetMapping;
  }

  void setScript(String script) {
    this.script = script;
  }

  void setVersion(String version) {
    this.version = version;
  }
}
