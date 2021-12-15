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
package org.corant.modules.query.shared.dynamic.jsonpredicate;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.corant.modules.json.expression.predicate.EvaluationContext;
import org.corant.modules.json.expression.predicate.FunctionResolver;
import org.corant.modules.json.expression.predicate.Node;
import org.corant.modules.json.expression.predicate.PredicateParser;
import org.corant.modules.json.expression.predicate.ast.ASTFunctionNode;
import org.corant.modules.json.expression.predicate.ast.ASTNode;
import org.corant.modules.json.expression.predicate.ast.ASTNodeBuilder;
import org.corant.modules.json.expression.predicate.ast.ASTVariableNode;
import org.corant.modules.json.expression.predicate.ast.ASTVariableNode.ASTDefaultVariableNode;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午3:00:31
 *
 */
@Singleton
public class JsonPredicateScriptProcessor implements ScriptProcessor {

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
      PredicateParser.resolveFunction().collect(Collectors.toList());

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
    return script != null && script.getType() == ScriptType.JPE;
  }

  @SuppressWarnings("unchecked")
  protected Function<ParameterAndResultPair, Object> createInjectFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Node<Boolean> ast = PredicateParser.parse(code, MyASTNodeBuilder.INST);
    return p -> {
      List<Map<Object, Object>> parentResults = (List<Map<Object, Object>>) p.parentResult;
      List<Map<Object, Object>> fetchResults = (List<Map<Object, Object>>) p.fetchedResult;
      MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
      for (Map<Object, Object> r : parentResults) {
        List<Map<Object, Object>> injectResults = new ArrayList<>();
        for (Map<Object, Object> fr : fetchResults) {
          if (ast.getValue(evalCtx.reset(r, fr))) {
            injectResults.add(fr);
            if (!fetchQuery.isMultiRecords()) {
              break;
            }
          }
        }
        if (fetchQuery.isMultiRecords()) {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(), injectResults);
        } else {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(),
              isNotEmpty(injectResults) ? injectResults.get(0) : null);
        }
      }
      return null;
    };
  }

  @SuppressWarnings("unchecked")
  protected Function<ParameterAndResult, Object> createPreFetchFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Node<Boolean> ast = PredicateParser.parse(code, MyASTNodeBuilder.INST);
    return p -> {
      Map<Object, Object> r = (Map<Object, Object>) p.result;
      MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
      return ast.getValue(evalCtx.reset(r, null));
    };
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

    public EvaluationContext reset(Map<Object, Object> parentResult,
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

}
