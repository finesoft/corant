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
package org.corant.suites.query.jpql;

import static org.corant.shared.util.Empties.isEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.jpql.JpqlNamedQueryResolver.JpqlQuerier;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultJpqlNamedQuerier implements JpqlQuerier {

  protected final String name;
  protected final String script;
  protected Object[] convertedParams;
  protected final Class<?> resultClass;
  protected final List<QueryHint> hints = new ArrayList<>();
  protected final Map<String, String> properties = new LinkedHashMap<>();

  /**
   * @param name
   * @param script
   * @param resultClass
   * @param properties
   */
  public DefaultJpqlNamedQuerier(String name, String script, Class<?> resultClass,
      Map<String, String> properties) {
    super();
    this.name = name;
    this.script = script.replaceAll("[\\t\\n\\r]", " ");
    this.resultClass = resultClass;
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  /**
   * @param name
   * @param script
   * @param convertedParams
   * @param resultClass
   * @param hints
   * @param properties
   */
  public DefaultJpqlNamedQuerier(String name, String script, Object[] convertedParams,
      Class<?> resultClass, List<QueryHint> hints, Map<String, String> properties) {
    this(name, script, resultClass, properties);
    setConvertedParams(convertedParams);
    if (!isEmpty(hints)) {
      for (QueryHint qh : hints) {
        this.hints.add(qh);
      }
    }
  }

  @Override
  public Object[] getConvertedParameters() {
    return Arrays.copyOf(convertedParams, convertedParams.length);
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return Collections.emptyList();
  }

  @Override
  public List<QueryHint> getHints() {
    return hints;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Class<T> getResultClass() {
    return (Class<T>) resultClass;
  }

  @Override
  public String getScript() {
    return script;
  }

  void setConvertedParams(Object[] param) {
    convertedParams = param == null ? new Object[0] : Arrays.copyOf(param, param.length);
  }

  DefaultJpqlNamedQuerier withParam(Object[] convertedParams) {
    setConvertedParams(convertedParams);
    return this;
  }
}