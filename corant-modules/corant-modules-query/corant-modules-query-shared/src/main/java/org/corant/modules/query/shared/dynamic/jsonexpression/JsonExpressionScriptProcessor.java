/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.shared.dynamic.jsonexpression;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.corant.modules.json.Jsons;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.SimpleParser;
import org.corant.modules.json.expression.ast.ASTFunctionNode;
import org.corant.modules.json.expression.ast.ASTNode;
import org.corant.modules.json.expression.ast.ASTNodeBuilder;
import org.corant.modules.json.expression.ast.ASTVariableNode;
import org.corant.modules.json.expression.ast.ASTVariableNode.ASTDefaultVariableNode;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午3:00:31
 *
 */
@Singleton
public class JsonExpressionScriptProcessor implements ScriptProcessor {

  public static final String FILTER_KEY = "filter";
  public static final String PROJECTION_KEY = "projection";
  public static final String PROJECTION_RENAME_KEY = "rename";
  public static final String PROJECTION_TYPE_KEY = "type";

  public static final String PARENT_RESULT_VAR_PREFIX =
      RESULT_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int PARENT_RESULT_VAR_PREFIX_LEN = PARENT_RESULT_VAR_PREFIX.length();
  public static final String FETCH_RESULT_VAR_PREFIX =
      FETCHED_RESULT_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int FETCH_RESULT_VAR_PREFIX_LEN = FETCH_RESULT_VAR_PREFIX.length();
  public static final String PARAMETER_VAR_PREFIX =
      PARAMETER_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int PARAMETER_VAR_PREFIX_LEN = PARAMETER_VAR_PREFIX.length();

  static final Map<String, Function<ParameterAndResultPair, Object>> injFuns =
      new ConcurrentHashMap<>();
  static final Map<String, Function<ParameterAndResult, Object>> pedFuns =
      new ConcurrentHashMap<>();
  static final List<FunctionResolver> functionResolvers =
      SimpleParser.resolveFunction().collect(Collectors.toList());

  @Inject
  protected QueryObjectMapper mapper;

  @Override
  public Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getInjectionScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return injFuns.computeIfAbsent(script.getId(), k -> createInjectFuns(fetchQuery, script));
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getPredicateScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return pedFuns.computeIfAbsent(script.getId(), k -> createPreFetchFuns(fetchQuery, script));
    }
    return null;
  }

  @Override
  public boolean supports(Script script) {
    return script != null && script.getType() == ScriptType.JSE;
  }

  protected Function<ParameterAndResultPair, Object> createInjectFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Pair<Node<Boolean>, Projector> eval = resolveInjectScript(code);
    final Node<Boolean> filter = eval.left();
    final Projector projector = eval.right();
    return p -> {
      List<Map<Object, Object>> parentResults = forceCast(p.parentResult);
      List<Map<Object, Object>> fetchResults = forceCast(p.fetchedResult);
      MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
      for (Map<Object, Object> r : parentResults) {
        List<Object> injectResults = new ArrayList<>();
        if (filter == null) {
          if (!fetchQuery.isMultiRecords()) {
            injectResults.add(fetchResults.get(0));
          } else {
            injectResults.addAll(fetchResults);
          }
        } else {
          for (Map<Object, Object> fr : fetchResults) {
            if (filter.getValue(evalCtx.link(r, fr))) {
              injectResults.add(fr);
              if (!fetchQuery.isMultiRecords()) {
                break;
              }
            }
          }
        }
        if (projector != null && !injectResults.isEmpty()) {
          injectResults = projector.apply(injectResults, mapper);
        }
        if (isNotEmpty(fetchQuery.getInjectPropertyNamePath())) {
          if (fetchQuery.isMultiRecords()) {
            mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(), injectResults);
          } else {
            mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(),
                isNotEmpty(injectResults) ? injectResults.get(0) : null);
          }
        } else {
          for (Object injectResult : injectResults) {
            if (injectResult != null) {
              Map<Object, Object> injectResultMap = forceCast(injectResult);
              r.putAll(injectResultMap);
              if (!fetchQuery.isMultiRecords()) {
                break;
              }
            }
          }
        }
      }
      return null;
    };
  }

  @SuppressWarnings("unchecked")
  protected Function<ParameterAndResult, Object> createPreFetchFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Node<Boolean> ast = (Node<Boolean>) SimpleParser.parse(code, MyASTNodeBuilder.INST);
    return p -> {
      Map<Object, Object> r = (Map<Object, Object>) p.result;
      MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
      return ast.getValue(evalCtx.link(r, null));
    };
  }

  protected Projector resolveInjectProjector(Map<String, Object> projectionMap) {
    Set<Mapping> mappings = new LinkedHashSet<>();
    projectionMap.forEach((k, v) -> {
      if (k != null && v != null) {
        String key = k.toString();
        String[] keyPath = split(key, Names.NAME_SPACE_SEPARATORS, true, true);
        if (keyPath.length > 0) {
          if (v instanceof Map) {
            Map<?, ?> vm = (Map<?, ?>) v;
            String name = getMapString(vm, PROJECTION_RENAME_KEY, key);
            String[] rename = split(name, Names.NAME_SPACE_SEPARATORS, true, true);
            String typeName = getMapString(vm, PROJECTION_TYPE_KEY);
            Class<?> type = typeName != null ? asClass(typeName) : null;
            mappings.add(new Mapping(keyPath, rename, type));
          } else if (toBoolean(v)) {
            mappings.add(new Mapping(keyPath, keyPath, null));
          }
        }
      }
    });
    if (isEmpty(mappings)) {
      throw new QueryRuntimeException("The projection can't empty!");
    }
    return new Projector(mappings);
  }

  protected Pair<Node<Boolean>, Projector> resolveInjectScript(String code) {
    final Map<String, Object> root = Jsons.fromString(code);
    Map<String, Object> filterMap = getMapMap(root, FILTER_KEY);
    Map<String, Object> projectionMap = getMapMap(root, PROJECTION_KEY);
    Node<Boolean> filter = null;
    Projector projector = null;
    if (filterMap != null) {
      filter = forceCast(SimpleParser.parse(filterMap, MyASTNodeBuilder.INST));
    }
    if (projectionMap != null) {
      projector = resolveInjectProjector(projectionMap);
    }
    if (filter == null && projector == null) {
      filter = forceCast(SimpleParser.parse(root, MyASTNodeBuilder.INST));
    }
    return Pair.of(filter, projector);
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:13:21
   *
   */
  static class Mapping {
    final String[] extractPath;
    final String[] injectPath;
    final Class<?> type;

    public Mapping(String[] extractPath, String[] injectPath, Class<?> type) {
      this.extractPath = extractPath;
      this.injectPath = injectPath;
      this.type = type;
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
      Mapping other = (Mapping) obj;
      return Arrays.equals(extractPath, other.extractPath);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + Arrays.hashCode(extractPath);
    }

  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:19:53
   *
   */
  static class MyASTNodeBuilder implements ASTNodeBuilder {

    static final MyASTNodeBuilder INST = new MyASTNodeBuilder();

    @Override
    public ASTNode<?> build(Object token) {
      ASTNode<?> node = ASTNodeBuilder.DFLT.build(token);
      if (node instanceof ASTVariableNode) {
        return new MyASTVariableNode(((ASTVariableNode) node).getName());
      }
      return node;
    }

  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:19:55
   *
   */
  static class MyASTVariableNode extends ASTDefaultVariableNode {

    private final Object[] namePath;

    public MyASTVariableNode(String name) {
      super(name);
      if (name.startsWith(PARENT_RESULT_VAR_PREFIX)) {
        namePath = split(name.substring(PARENT_RESULT_VAR_PREFIX_LEN), Names.NAME_SPACE_SEPARATORS);
      } else if (name.startsWith(FETCH_RESULT_VAR_PREFIX)) {
        namePath = split(name.substring(FETCH_RESULT_VAR_PREFIX_LEN), Names.NAME_SPACE_SEPARATORS);
      } else {
        namePath = split(name.substring(PARAMETER_VAR_PREFIX_LEN), Names.NAME_SPACE_SEPARATORS);
      }
    }

    Object[] getNamePath() {
      return namePath;
    }

  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:19:59
   *
   */
  static class MyEvaluationContext implements EvaluationContext {

    Map<Object, Object> parentResult;
    Map<Object, Object> fetchResult;
    Map<Object, Object> queryParameterMap;
    boolean queryParamMapResolved;
    final Object queryParameter;
    final QueryObjectMapper objectMapper;
    final List<FunctionResolver> functionResolvers;

    public MyEvaluationContext(QueryObjectMapper objectMapper, Object queryParameter,
        List<FunctionResolver> functionResolvers) {
      this.objectMapper = objectMapper;
      this.queryParameter = queryParameter;
      this.functionResolvers = functionResolvers;
    }

    public EvaluationContext link(Map<Object, Object> parentResult,
        Map<Object, Object> fetchResult) {
      this.parentResult = parentResult;
      this.fetchResult = fetchResult;
      return this;
    }

    @Override
    public Function<Object[], Object> resolveFunction(Node<?> node) {
      ASTFunctionNode myNode = (ASTFunctionNode) node;
      return functionResolvers.stream().filter(fr -> fr.supports(myNode.getName()))
          .min(Sortable::compare).orElseThrow(NotSupportedException::new).resolve(myNode.getName());
    }

    @Override
    public Object resolveVariableValue(Node<?> node) {
      MyASTVariableNode myNode = (MyASTVariableNode) node;
      if (myNode.getName().startsWith(PARENT_RESULT_VAR_PREFIX)) {
        return objectMapper.getMappedValue(parentResult, myNode.getNamePath());
      } else if (myNode.getName().startsWith(FETCH_RESULT_VAR_PREFIX)) {
        return objectMapper.getMappedValue(fetchResult, myNode.getNamePath());
      } else {
        if (!queryParamMapResolved) {
          queryParameterMap = objectMapper.toObject(queryParameter, Map.class);
        }
        return objectMapper.getMappedValue(queryParameterMap, myNode.getNamePath());
      }
    }

  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:17:15
   *
   */
  static class Projector implements BiFunction<List<Object>, QueryObjectMapper, List<Object>> {

    final Collection<Mapping> mappings;

    public Projector(Collection<Mapping> mappings) {
      this.mappings = Collections.unmodifiableCollection(mappings);
    }

    @Override
    public List<Object> apply(List<Object> fetchResults, QueryObjectMapper objectMapper) {
      List<Object> results = new ArrayList<>();
      for (Object fetchResult : fetchResults) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Mapping mapping : mappings) {
          Object extracted = objectMapper.getMappedValue(fetchResult, mapping.extractPath);
          if (mapping.type != null) {
            extracted = toObject(extracted, mapping.type);
          }
          objectMapper.putMappedValue(result, mapping.injectPath, extracted);
        }
        results.add(result);
      }
      return results;
    }

  }

}
