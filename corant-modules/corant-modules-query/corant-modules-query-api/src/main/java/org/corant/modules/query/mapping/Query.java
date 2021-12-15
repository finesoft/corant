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
  private Map<String, String> properties = new HashMap<>();
  private String mappingFilePath;
  private String macroScript;// FIXME temporary

  public Query() {}

  public Query(String mappingFilePath) {
    this();
    setMappingFilePath(mappingFilePath);
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
   * @param mappingFilePath
   * @param macroScript
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
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
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
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the fetchQueries
   */
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  /**
   * @return the hints
   */
  public List<QueryHint> getHints() {
    return hints;
  }

  /**
   *
   * @return the macroScript
   */
  public String getMacroScript() {
    return macroScript;
  }

  /**
   * The mapping file path where this query come from
   *
   * @return getMappingFilePath
   */
  public String getMappingFilePath() {
    return mappingFilePath;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public Map<String, Class<?>> getParamConvertSchema() {
    return Collections.unmodifiableMap(getParamMappings().entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getType())));
  }

  /**
   * @return the paramMappings
   */
  public Map<String, ParameterMapping> getParamMappings() {
    return paramMappings;
  }

  /**
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  public Object getProperty(String name) {
    return getProperties() == null ? null : getProperties().get(name);
  }

  /**
   * Returns the property value of the specified type
   *
   * @param <T> property type
   * @param name property name
   * @param cls property type class
   * @return getProperty
   */
  public <T> T getProperty(String name, Class<T> cls) {
    return toObject(getProperty(name), cls);
  }

  /**
   * Returns the property value of the specified type, if not found or be found is null return
   * alternative value.
   *
   * @param <T>
   * @param name
   * @param cls
   * @param altVal if not found or be found is null return this value
   * @return getProperty
   */
  public <T> T getProperty(String name, Class<T> cls, T altVal) {
    return defaultObject(getProperty(name, cls), altVal);
  }

  /**
   * Return the result class, if not setting return java.util.Map.class
   *
   * @return the resultClass
   */
  public Class<?> getResultClass() {
    return resultClass;
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
  public Script getScript() {
    return script;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  public List<String> getVersionedFetchQueryNames() {
    return fetchQueries.stream().map(f -> f.getReferenceQuery().getVersionedName())
        .collect(Collectors.toList());
  }

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
      fetchQueries.forEach(FetchQuery::postConstuct);
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

  /**
   *
   * @param macroScript the macroScript to set
   */
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
  }

  protected void setProperties(Map<String, String> properties) {
    this.properties = properties;
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

  protected void setVersion(String version) {
    this.version = version;
  }

  public enum QueryType {
    SQL, MG, JPQL, ES, CAS
  }
}
