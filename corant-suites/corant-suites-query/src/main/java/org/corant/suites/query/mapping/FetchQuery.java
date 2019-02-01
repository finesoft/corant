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
package org.corant.suites.query.mapping;

import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * asosat-query
 *
 * @author bingo 上午10:26:45
 *
 */
public class FetchQuery implements Serializable {

  private static final long serialVersionUID = 449192431797295206L;
  private String referenceQuery;
  private String injectPropertyName;
  private Class<?> resultClass = Map.class;
  private int maxSize;
  private List<FetchQueryParameter> parameters = new ArrayList<>();
  private String referenceQueryversion = "";
  private boolean multiRecords = true;

  /**
   * @return the injectPropertyName
   */
  public String getInjectPropertyName() {
    return injectPropertyName;
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
   * @return the referenceQuery
   */
  public String getReferenceQuery() {
    return referenceQuery;
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

  public boolean isMultiRecords() {
    return multiRecords;
  }

  void setInjectPropertyName(String injectPropertyName) {
    this.injectPropertyName = injectPropertyName;
  }

  void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  void setMultiRecords(boolean multiRecords) {
    this.multiRecords = multiRecords;
  }

  void setReferenceQuery(String referenceQuery) {
    this.referenceQuery = referenceQuery;
  }

  void setReferenceQueryversion(String referenceQueryversion) {
    this.referenceQueryversion = referenceQueryversion;
  }

  void setResultClass(Class<?> resultClass) {
    this.resultClass = resultClass;
  }

  public static class FetchQueryParameter implements Serializable {

    private static final long serialVersionUID = 5013658267151165784L;

    private String name;
    private String sourceName;
    private FetchQueryParameterSource source;
    private String value;

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

    public String getValue() {
      return value;
    }

    void setName(String name) {
      this.name = name;
    }

    void setSource(FetchQueryParameterSource source) {
      this.source = source;
    }

    void setSourceName(String sourceName) {
      this.sourceName = sourceName;
    }

    void setValue(String value) {
      this.value = value;
    }

  }

  public enum FetchQueryParameterSource {
    P, R, C
  }
}
