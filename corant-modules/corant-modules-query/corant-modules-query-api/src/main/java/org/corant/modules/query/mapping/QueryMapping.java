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

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.newHashMap;
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
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Script.ScriptType;

/**
 * corant-modules-query-api
 * <p>
 * In general, each query mapping corresponds to a query program resource, and each program resource
 * contains some queries.
 *
 * @author bingo 下午3:41:30
 */
public class QueryMapping {

  protected String url;
  protected List<Query> queries = new ArrayList<>();
  protected Map<String, ParameterMapping> paramMappings = new HashMap<>();
  protected String commonSegment;

  public QueryMapping() {}

  public QueryMapping(String url, String commonSegment) {
    this.url = url;
    this.commonSegment = commonSegment;
  }

  public String getCommonSegment() {
    return commonSegment;
  }

  public Map<String, ParameterMapping> getParamMappings() {
    return paramMappings;
  }

  public List<Query> getQueries() {
    return queries;
  }

  public String getUrl() {
    return url;
  }

  public List<String> selfValidate() {
    List<String> brokens = new ArrayList<>();
    // validate parameters-mapping elements
    getParamMappings().values().forEach(p -> {
      if (p.getType() == null) {
        brokens.add(format(
            "The 'type' attribute: [query-mappings > parameters-mapping > parameter-type-mapping(%s) > type] can not null! query file [%s]",
            p.getName(), url));
      }
    });
    if (isEmpty(getQueries())) {
      brokens.add(format("The query file [%s] must have 'query' elements!", getUrl()));
    }
    Set<String> queryNames = new HashSet<>();
    // validate query elements
    getQueries().forEach(q -> {
      if (isBlank(q.getName())) {
        brokens.add(format("The query file [%s] has noname 'query' element!", getUrl()));
      }
      // since 2024-08-31
      if (!q.isInline() && q.getName().contains(SchemaNames.QUE_NAME_SPACE_SEPARATOR)) {
        brokens.add(format("The query name [%s] contains illegality string [%s], query  file [%s]",
            q.getName(), SchemaNames.QUE_NAME_SPACE_SEPARATOR, getUrl()));
      }
      if (q.getResultClass() == null) {
        brokens.add(format(
            "The 'result-class' attribute: [query-mappings > query(%s) -> result-class] can not null! query file [%s]",
            q.getName(), getUrl()));
      }
      if (!q.getScript().isValid()) {
        brokens.add(format(
            "The 'script' element: [query-mappings > query(%s) > script] can not null! query file [%s]",
            q.getName(), getUrl()));
      }

      if (queryNames.contains(q.getVersionedName())) {
        brokens.add(format(
            "The 'name' attribute: [query-mappings > query(%s) -> name] can not repeat! query file [%s]",
            q.getName(), getUrl()));
      } else {
        queryNames.add(q.getVersionedName());
      }
      // validate fetch queries elements
      q.getFetchQueries().forEach(fq -> {
        Set<String> injectProNames = new HashSet<>();
        if (isBlank(fq.getQueryReference().getVersionedName())) {
          brokens.add(format(
              "The 'reference-query' attribute: [query-mappings > query(%s) > fetch-query -> reference-query] can not null! query file [%s]",
              q.getName(), getUrl()));
        }
        if (isBlank(fq.getInjectPropertyName()) && !fq.getInjectionScript().isValid()) {
          brokens.add(format(
              "The 'fetch-query' element: [query-mappings > query(%s) > fetch-query(%s)] must contain either 'inject-property-name' attribute or 'injection-script' element! query file [%s]",
              q.getName(), fq.getQueryReference().getName(), getUrl()));
        } else if (isNotBlank(fq.getInjectPropertyName())
            && !injectProNames.add(fq.getInjectPropertyName())) {
          brokens.add(format(
              "The 'inject-property-name' attribute: [query-mappings > query(%s) > fetch-query(%s) -> inject-property-name(%s)] can not repeat! query file [%s]",
              q.getName(), fq.getQueryReference().getName(), fq.getInjectPropertyName(), getUrl()));
        } else if (fq.getInjectionScript().isValid()
            && fq.getInjectionScript().getType() == ScriptType.FM) {
          brokens.add(format(
              "The 'type' attribute: [query-mappings > query(%s) > fetch-query(%s) > injection-script] can't be 'FM'! query file [%s]",
              q.getName(), fq.getQueryReference().getName(), getUrl()));
        }

        // // comment: allow for more liberal injections
        // if (isBlank(fq.getInjectPropertyName())) {
        // brokens.add(format(
        // "The 'inject-property-name' attribute of 'fetch-query' in query element [%s] in query
        // file [%s] can not null!",
        // q.getName(), getUrl()));
        // } else if (injectProNames.contains(fq.getInjectPropertyName())) {
        // brokens.add(format(
        // "The 'fetch-query' [%s] with 'inject-property-name' [%s] in query element [%s] in query
        // file [%s] can not repeat!",
        // fq.getReferenceQuery(), fq.getInjectPropertyName(), q.getName(), getUrl()));
        // } else {
        // injectProNames.add(fq.getInjectPropertyName());
        // }

        if (areEqual(q.getVersionedName(), fq.getQueryReference().getVersionedName())) {
          brokens.add(format(
              "The 'reference-query' attribute: [query-mappings > query(%s) > fetch-query -> reference-query(%s)] can not reference the parent query! query file [%s]",
              q.getName(), fq.getQueryReference().getName(), getUrl()));
        }
        // validate fetch queries parameter
        Set<String> paramNames = new HashSet<>();
        Set<String> paramGroups = new HashSet<>();
        fq.getParameters().forEach(fqp -> {
          if (isBlank(fqp.getName())) {
            brokens.add(format(
                "The 'name' attribute: [query-mappings > query(%s) > fetch-query(%s) > parameter -> name] can not null! query file [%s]",
                q.getName(), fq.getQueryReference().getName(), getUrl()));
          }
          if (!paramNames.add(fqp.getName())) {
            brokens.add(format(
                "The 'name' attribute: [query-mappings > query(%s) > fetch-query(%s) > parameter -> name(%s)] can not repeat! query file [%s]",
                q.getName(), fq.getQueryReference().getName(), fqp.getName(), getUrl()));
          }
          if (isNull(fqp.getSource())) {
            brokens.add(format(
                "The 'source' attribute: [query-mappings > query(%s) > fetch-query(%s) > parameter(%s) -> source] can not null! query file [%s]",
                q.getName(), fq.getQueryReference().getName(), fqp.getName(), getUrl()));
          } else if ((fqp.getSource() == FetchQueryParameterSource.R
              || fqp.getSource() == FetchQueryParameterSource.P) && isBlank(fqp.getSourceName())) {
            brokens.add(format(
                "The 'source-name' attribute: [query-mappings > query(%s) > fetch-query(%s) > parameter(%s) -> source] can not null! query file [%s]",
                q.getName(), fq.getQueryReference().getName(), fqp.getName(), getUrl()));
          }
          if (isNotBlank(fqp.getGroup())) {
            paramGroups.add(fqp.getGroup());
          }
          if (paramGroups.contains(fqp.getName())) {
            brokens.add(format(
                "The 'name' attribute: [query-mappings > query(%s) > fetch-query(%s) > parameter -> name(%s)] and the 'group' attribute can't have the same value! query file [%s]",
                q.getName(), fq.getQueryReference().getName(), fqp.getName(), getUrl()));
          }
        });
        paramNames.clear();
        paramGroups.clear();
      });
    });
    queryNames.clear();
    return brokens;
  }

  protected void assembly() { // FIXME
    List<Query> tempQueries = new ArrayList<>();
    Map<String, ParameterMapping> tempParaMapping = new HashMap<>();
    if (isNotEmpty(queries)) {
      for (Query q : queries) {
        if (isNotBlank(commonSegment)) {
          q.setMacroScript(commonSegment);
        }
        tempQueries.add(q);
        assemblyFetchQuery(q, tempQueries);
      }
    }
    if (isNotEmpty(paramMappings)) {
      tempParaMapping.putAll(paramMappings);
    }
    queries = unmodifiableList(tempQueries);
    paramMappings = unmodifiableMap(tempParaMapping);
  }

  protected void assemblyFetchQuery(Query query, List<Query> queries) {
    List<FetchQuery> fetchQueries = query.getFetchQueries();
    if (isEmpty(fetchQueries)) {
      return;
    }
    for (FetchQuery fq : fetchQueries) {
      if (fq.getInlineQueryName() != null) {
        shouldBeTrue(
            fq.getReferenceQueryName() == null && fq.getReferenceQueryQualifier() == null
                && fq.getReferenceQueryType() == null && fq.getReferenceQueryVersion() == null
                && fq.getInlineQueryScript().isValid(),
            () -> new QueryRuntimeException(
                "When using inline query [%s > %s], the referenced query can't be set at the same time and the inline query script must be valid.",
                query.getName(), fq.getInlineQueryName()));
        // here we need to extract the in-line query
        Query inlineQuery = new Query();
        String inlineQueryName = query.getName().concat(SchemaNames.QUE_NAME_SPACE_SEPARATOR)
            .concat(fq.getInlineQueryName());
        inlineQuery.setInline(true);
        inlineQuery.setName(inlineQueryName);
        inlineQuery.setQualifier(fq.getInlineQueryQualifier());
        inlineQuery.setType(fq.getInlineQueryType());
        inlineQuery.setScript(fq.getInlineQueryScript());
        inlineQuery.setCache(fq.isInlineQueryCache());
        inlineQuery.setDescription(fq.getDescription());
        if (isNotBlank(commonSegment)) {
          inlineQuery.setMacroScript(commonSegment);
        }
        inlineQuery.setMappingFilePath(query.getMappingFilePath());
        // FIXME copy parent query
        inlineQuery.setParamMappings(newHashMap(query.getParamMappings()));
        inlineQuery.setProperties(newHashMap(query.getProperties()));
        if (fq.getProperties() != null) {
          // copy and rewrite parent query properties
          fq.getProperties().forEach(inlineQuery::addProperty);
        }
        inlineQuery.setResultClass(fq.getResultClass());
        List<FetchQuery> inlineFetchQueries = fq.getFetchQueries();
        if (isNotEmpty(inlineFetchQueries)) {
          for (FetchQuery ifq : inlineFetchQueries) {
            inlineQuery.addFetchQuery(ifq);
          }
          assemblyFetchQuery(inlineQuery, queries);
        }
        fq.setQueryReference(new QueryReference(inlineQuery));
        queries.add(inlineQuery);
      } else {
        shouldBeTrue(
            fq.getReferenceQueryName() != null && fq.getInlineQueryName() == null
                && fq.getInlineQueryQualifier() == null && fq.getInlineQueryType() == null
                && (fq.getInlineQueryScript() == null || !fq.getInlineQueryScript().isValid())
                && isEmpty(fq.getFetchQueries()),
            () -> new QueryRuntimeException(
                "When using referenced query [%s > %s], inline query and inline query script can't be set at the same time and can't contain sub fetch queries.",
                query.getName(), fq.getReferenceQueryName()));
        QueryReference referenceQuery = new QueryReference();
        referenceQuery.setName(fq.getReferenceQueryName());
        referenceQuery.setType(fq.getReferenceQueryType());
        referenceQuery.setQualifier(fq.getReferenceQueryQualifier());
        referenceQuery.setVersion(fq.getReferenceQueryVersion());
        fq.setQueryReference(referenceQuery);
      }
    }
  }

  protected void setCommonSegment(String commonSegment) {
    this.commonSegment = commonSegment;
  }
}
