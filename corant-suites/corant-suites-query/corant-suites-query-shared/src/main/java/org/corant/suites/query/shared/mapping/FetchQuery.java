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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.split;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Strings;
import org.corant.suites.query.shared.mapping.Query.QueryType;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:26:45
 *
 */
public class FetchQuery implements Serializable {

  private static final long serialVersionUID = 449192431797295206L;
  private QueryReference referenceQuery = new QueryReference();
  private String injectPropertyName;
  private String[] injectPropertyNamePath = Strings.EMPTY_ARRAY;
  private Class<?> resultClass = Map.class;
  private int maxSize = 1024;
  private List<FetchQueryParameter> parameters = new ArrayList<>();
  private boolean multiRecords = true;
  private Script predicateScript = new Script();
  private Script injectionScript = new Script();
  private boolean eagerInject = true;
  private final String id = UUID.randomUUID().toString();

  public FetchQuery() {
    super();
  }

  /**
   * @param referenceQuery
   * @param injectPropertyName
   * @param resultClass
   * @param maxSize
   * @param parameters
   * @param multiRecords
   * @param predicate
   * @param injection
   * @param eagerInject
   */
  public FetchQuery(QueryReference referenceQuery, String injectPropertyName, Class<?> resultClass,
      int maxSize, List<FetchQueryParameter> parameters, boolean multiRecords, Script predicate,
      Script injection, boolean eagerInject) {
    super();
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
  public QueryReference getReferenceQuery() {
    return referenceQuery;
  }

  /**
   * @return the resultClass
   */
  public Class<?> getResultClass() {
    return resultClass;
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
   * Make query immutable
   */
  protected void postConstuct() {
    parameters =
        parameters == null ? Collections.emptyList() : Collections.unmodifiableList(parameters);
  }

  /**
   *
   * @param eagerInject the eagerInject to set
   */
  protected void setEagerInject(boolean eagerInject) {
    this.eagerInject = eagerInject;
  }

  protected void setInjectionScript(Script injection) {
    injectionScript = defaultObject(injection, Script.EMPTY);
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
    predicateScript = defaultObject(predicate, Script.EMPTY);
  }

  /**
   *
   * @param referenceQuery the referenceQuery to set
   */
  protected void setReferenceQuery(QueryReference referenceQuery) {
    this.referenceQuery = referenceQuery;
  }

  protected void setReferenceQueryName(String referenceQueryName) {
    referenceQuery.setName(referenceQueryName);
  }

  /**
   *
   * @param referenceQueryQualifier the referenceQueryQualifier to set
   */
  protected void setReferenceQueryQualifier(String referenceQueryQualifier) {
    referenceQuery.setQualifier(referenceQueryQualifier);
  }

  /**
   *
   * @param referenceQueryType the referenceQueryType to set
   */
  protected void setReferenceQueryType(QueryType referenceQueryType) {
    referenceQuery.setType(referenceQueryType);
  }

  protected void setReferenceQueryversion(String referenceQueryversion) {
    referenceQuery.setVersion(referenceQueryversion);
  }

  protected void setResultClass(Class<?> resultClass) {
    this.resultClass = defaultObject(resultClass, Map.class);
  }

  public static class FetchQueryParameter implements Serializable {

    private static final long serialVersionUID = 5013658267151165784L;

    private String name;
    private String sourceName;
    private String[] sourceNamePath = Strings.EMPTY_ARRAY;
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
