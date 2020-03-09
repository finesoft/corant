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
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.corant.shared.normal.Names;
import org.corant.suites.query.shared.mapping.Query.QueryType;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:26:45
 *
 */
public class FetchQuery implements Serializable {

  private static final long serialVersionUID = 449192431797295206L;
  private String referenceQuery;
  private QueryType referenceQueryType;
  private String referenceQueryQualifier;
  private String injectPropertyName;
  private String[] injectPropertyNamePath = new String[0];
  private Class<?> resultClass = Map.class;
  private int maxSize = 1024;
  private List<FetchQueryParameter> parameters = new ArrayList<>();
  private String referenceQueryversion = "";
  private boolean multiRecords = true;
  private Script predicateScript = new Script();
  private Script injectionScript = new Script();
  private boolean eagerInject = true;
  private final String id = UUID.randomUUID().toString();

  public FetchQuery() {
    super();
  }

  /**
   *
   * @param referenceQuery
   * @param injectPropertyName
   * @param resultClass
   * @param maxSize
   * @param parameters
   * @param referenceQueryversion
   * @param multiRecords
   * @param predicate
   * @param injection
   * @param eagerInject
   * @param referenceQueryType
   * @param referenceQueryQualifier
   */
  public FetchQuery(String referenceQuery, String injectPropertyName, Class<?> resultClass,
      int maxSize, List<FetchQueryParameter> parameters, String referenceQueryversion,
      boolean multiRecords, Script predicate, Script injection, boolean eagerInject,
      QueryType referenceQueryType, String referenceQueryQualifier) {
    super();
    setReferenceQuery(referenceQuery);
    setInjectPropertyName(injectPropertyName);
    setResultClass(resultClass);
    setMaxSize(maxSize);
    if (parameters != null) {
      this.parameters.addAll(parameters);
    }
    setReferenceQueryversion(referenceQueryversion);
    setMultiRecords(multiRecords);
    if (predicate != null) {
      setPredicateScript(predicate);
    }
    if (injection != null) {
      setInjectionScript(injection);
    }
    setEagerInject(eagerInject);
    setReferenceQueryType(referenceQueryType);
    setReferenceQueryQualifier(referenceQueryQualifier);
  }

  /**
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  public Script getInjectionScript() {
    return injectionScript;
  }

  /**
   * @return the injectPropertyName
   */
  public String getInjectPropertyName() {
    return injectPropertyName;
  }

  /**
   *
   * @return the injectPropertyNamePath
   */
  public String[] getInjectPropertyNamePath() {
    return injectPropertyNamePath;
  }

  /**
   * @return the maxSize
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * @return the parameters
   */
  public List<FetchQueryParameter> getParameters() {
    return parameters;
  }

  /**
   *
   * @return getPredicateScript
   */
  public Script getPredicateScript() {
    return predicateScript;
  }

  /**
   * @return the referenceQuery
   */
  public String getReferenceQuery() {
    return referenceQuery;
  }

  /**
   *
   * @return the referenceQueryQualifier
   */
  public String getReferenceQueryQualifier() {
    return referenceQueryQualifier;
  }

  /**
   *
   * @return the referenceQueryType
   */
  public QueryType getReferenceQueryType() {
    return referenceQueryType;
  }

  /**
   * @return the referenceQueryversion
   */
  public String getReferenceQueryversion() {
    return referenceQueryversion;
  }

  /**
   * @return the resultClass
   */
  public Class<?> getResultClass() {
    return resultClass;
  }

  public String getVersionedReferenceQueryName() {
    return defaultString(getReferenceQuery())
        + (isNotBlank(getReferenceQueryversion()) ? "_" + getReferenceQueryversion() : "");
  }

  /**
   *
   * @return the eagerInject
   */
  public boolean isEagerInject() {
    return eagerInject;
  }

  /**
   *
   * @return isMultiRecords
   */
  public boolean isMultiRecords() {
    return multiRecords;
  }

  protected void addParameter(FetchQueryParameter parameter) {
    parameters.add(parameter);
  }

  /**
   *
   * @param eagerInject the eagerInject to set
   */
  protected void setEagerInject(boolean eagerInject) {
    this.eagerInject = eagerInject;
  }

  protected void setInjectionScript(Script injection) {
    injectionScript = defaultObject(injection, new Script());
  }

  protected void setInjectPropertyName(String injectPropertyName) {
    this.injectPropertyName = injectPropertyName;
    injectPropertyNamePath = split(injectPropertyName, Names.NAME_SPACE_SEPARATORS, true, false);
  }

  protected void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  protected void setMultiRecords(boolean multiRecords) {
    this.multiRecords = multiRecords;
  }

  protected void setPredicateScript(Script predicate) {
    predicateScript = defaultObject(predicate, new Script());
  }

  protected void setReferenceQuery(String referenceQuery) {
    this.referenceQuery = referenceQuery;
  }

  /**
   *
   * @param referenceQueryQualifier the referenceQueryQualifier to set
   */
  protected void setReferenceQueryQualifier(String referenceQueryQualifier) {
    this.referenceQueryQualifier = referenceQueryQualifier;
  }

  /**
   *
   * @param referenceQueryType the referenceQueryType to set
   */
  protected void setReferenceQueryType(QueryType referenceQueryType) {
    this.referenceQueryType = referenceQueryType;
  }

  protected void setReferenceQueryversion(String referenceQueryversion) {
    this.referenceQueryversion = referenceQueryversion;
  }

  protected void setResultClass(Class<?> resultClass) {
    this.resultClass = defaultObject(resultClass, Map.class);
  }

  /**
   * Make query immutable
   */
  void immunize() {
    parameters =
        parameters == null ? Collections.emptyList() : Collections.unmodifiableList(parameters);
  }

  public static class FetchQueryParameter implements Serializable {

    private static final long serialVersionUID = 5013658267151165784L;

    private String name;
    private String sourceName;
    private String[] sourceNamePath = new String[0];
    private FetchQueryParameterSource source;
    private String value;
    private Class<?> type;
    private boolean distinct = true;

    public FetchQueryParameter() {
      super();
    }

    /**
     * @param name
     * @param sourceName
     * @param source
     * @param value
     * @param type
     * @param distinct
     */
    public FetchQueryParameter(String name, String sourceName, FetchQueryParameterSource source,
        String value, Class<?> type, boolean distinct) {
      super();
      setName(name);
      setSourceName(sourceName);
      setSource(source);
      setValue(value);
      setType(type);
      setDistinct(distinct);
    }

    /**
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the source
     */
    public FetchQueryParameterSource getSource() {
      return source;
    }

    /**
     * @return the sourceName
     */
    public String getSourceName() {
      return sourceName;
    }

    /**
     *
     * @return the sourceNamePath
     */
    public String[] getSourceNamePath() {
      return sourceNamePath;
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
     * @return getValue
     */
    public String getValue() {
      return value;
    }

    public boolean isDistinct() {
      return distinct;
    }

    protected void setDistinct(boolean distinct) {
      this.distinct = distinct;
    }

    protected void setName(String name) {
      this.name = name;
    }

    protected void setSource(FetchQueryParameterSource source) {
      this.source = source;
    }

    protected void setSourceName(String sourceName) {
      this.sourceName = sourceName;
      sourceNamePath = split(sourceName, Names.NAME_SPACE_SEPARATORS, true, false);
    }

    protected void setType(Class<?> type) {
      this.type = type;
    }

    protected void setValue(String value) {
      this.value = value;
    }

  }

  public enum FetchQueryParameterSource {
    P, R, C
  }

}
