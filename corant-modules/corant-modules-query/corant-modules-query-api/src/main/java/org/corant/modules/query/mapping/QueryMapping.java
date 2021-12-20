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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.isNull;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Script.ScriptType;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午3:41:30
 *
 */
public class QueryMapping {

  String url;
  final List<Query> queries = new ArrayList<>();
  final Map<String, ParameterMapping> paraMapping = new HashMap<>();
  String commonSegment;

  public QueryMapping() {}

  /**
   * @param url
   * @param commonSegment
   */
  public QueryMapping(String url, String commonSegment) {
    this.url = url;
    this.commonSegment = commonSegment;
  }

  /**
   * @return the commonSegment
   */
  public String getCommonSegment() {
    return commonSegment;
  }

  /**
   * @return the paraMapping
   */
  public Map<String, ParameterMapping> getParaMapping() {
    return paraMapping;
  }

  /**
   * @return the queries
   */
  public List<Query> getQueries() {
    return queries;
  }

  /**
   * @return the uri
   */
  public String getUrl() {
    return url;
  }

  public List<String> selfValidate() {
    List<String> brokens = new ArrayList<>();
    // validate parameters-mapping elements
    getParaMapping().values().forEach(p -> {
      if (p.getType() == null) {
        brokens.add(String.format(
            "The 'type' attribute of parameter entry element [%s] in query file [%s] can not null!",
            url, p.getName()));
      }
    });
    if (isEmpty(getQueries())) {
      brokens.add(String.format("The query file [%s] must have 'query' elements!", getUrl()));
    }
    Set<String> queryNames = new HashSet<>();
    // validate query elements
    getQueries().forEach(q -> {
      if (isBlank(q.getName())) {
        brokens.add(String.format("The query file [%s] has noname 'query' element!", getUrl()));
      }
      if (q.getResultClass() == null) {
        brokens.add(String.format(
            "The 'result-class' attribute of 'query' element [%s] in query file [%s] can not null!",
            q.getName(), getUrl()));
      }
      if (!q.getScript().isValid()) {
        brokens.add(String.format(
            "The 'script' element in 'query' element [%s] in query file [%s] can not null!",
            q.getName(), getUrl()));
      }
      if (q.getScript().getType() == ScriptType.JSE) {
        brokens.add(String.format(
            "The type [%s] of the 'script' element of the 'query' element [%s] in the query file [%s] not support!",
            q.getScript().getType().toString(), q.getName(), getUrl()));
      }
      if (queryNames.contains(q.getVersionedName())) {
        brokens.add(String.format(
            "The 'name' attribute of 'query' element [%s] in query file [%s] can not repeat!",
            q.getName(), getUrl()));
      } else {
        queryNames.add(q.getVersionedName());
      }
      // validate fetch queries elements
      q.getFetchQueries().forEach(fq -> {
        Set<String> injectProNames = new HashSet<>();
        if (isBlank(fq.getReferenceQuery().getVersionedName())) {
          brokens.add(String.format(
              "The 'reference-query' attribute of 'fetch-query' in query element [%s] in query file [%s] can not null!",
              q.getName(), getUrl()));
        }
        if (isBlank(fq.getInjectPropertyName()) && !fq.getInjectionScript().isValid()) {
          brokens.add(String.format(
              "The 'fetch-query' [%s] must contain either 'inject-property-name' attribute or 'injection-script' element in query element [%s] in query file [%s].",
              fq.getReferenceQuery(), q.getName(), getUrl()));
        } else if (isNotBlank(fq.getInjectPropertyName())
            && !injectProNames.add(fq.getInjectPropertyName())) {
          brokens.add(String.format(
              "The 'fetch-query' [%s] with 'inject-property-name' [%s] in query element [%s] in query file [%s] can not repeat!",
              fq.getReferenceQuery(), fq.getInjectPropertyName(), q.getName(), getUrl()));
        } else if (fq.getInjectionScript().isValid()
            && fq.getInjectionScript().getType() == ScriptType.FM) {
          brokens.add(String.format(
              "The script type [%s] can't be 'FM' which in 'fetch-query' [%s] 'injection-script' element in query element [%s] in query file [%s].",
              fq.getInjectionScript().getType().name(), fq.getReferenceQuery(), q.getName(),
              getUrl()));
        }

        // if (isBlank(fq.getInjectPropertyName())) {
        // brokens.add(String.format(
        // "The 'inject-property-name' attribute of 'fetch-query' in query element [%s] in query
        // file [%s] can not null!",
        // q.getName(), getUrl()));
        // } else if (injectProNames.contains(fq.getInjectPropertyName())) {
        // brokens.add(String.format(
        // "The 'fetch-query' [%s] with 'inject-property-name' [%s] in query element [%s] in query
        // file [%s] can not repeat!",
        // fq.getReferenceQuery(), fq.getInjectPropertyName(), q.getName(), getUrl()));
        // } else {
        // injectProNames.add(fq.getInjectPropertyName());
        // }

        if (areEqual(q.getVersionedName(), fq.getReferenceQuery().getVersionedName())) {
          brokens.add(String.format(
              "The 'fetch-query' [%s] in query element [%s] in query file [%s] can not reference the parent query!",
              fq.getReferenceQuery(), q.getName(), getUrl()));
        }
        // validate fetch queries parameter
        fq.getParameters().forEach(fqp -> {
          if (isBlank(fqp.getName())) {
            brokens.add(String.format(
                "The 'name' attribute of 'parameter' in fetch query [%s] in query element [%s] in query file [%s] can not null!",
                fq.getReferenceQuery(), q.getName(), getUrl()));
          }
          if (isNull(fqp.getSource())) {
            brokens.add(String.format(
                "The 'source' attribute of 'parameter' in fetch query [%s] in query element [%s] in query file [%s] can not null!",
                fq.getReferenceQuery(), q.getName(), getUrl()));
          } else if ((fqp.getSource() == FetchQueryParameterSource.R
              || fqp.getSource() == FetchQueryParameterSource.P) && isBlank(fqp.getSourceName())) {
            brokens.add(String.format(
                "The 'source-name' attribute of 'parameter' in fetch query [%s] in query element [%s] in query file [%s] can not null!",
                fq.getReferenceQuery(), q.getName(), getUrl()));
          }
        });
      });
    });
    return brokens;
  }

  /**
   *
   * @param commonSegment the commonSegment to set
   */
  protected void setCommonSegment(String commonSegment) {
    this.commonSegment = commonSegment;
  }

  void assembly() { // FIXME
    if (isNotBlank(commonSegment) && isNotEmpty(queries)) {
      for (Query q : queries) {
        q.setMacroScript(commonSegment);
      }
    }
  }
}
