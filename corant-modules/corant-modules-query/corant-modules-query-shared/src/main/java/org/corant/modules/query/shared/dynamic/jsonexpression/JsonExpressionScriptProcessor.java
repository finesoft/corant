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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static org.corant.shared.normal.Names.splitNameSpace;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.getMapBoolean;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapObject;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor.AbstractScriptProcessor;
import org.corant.modules.query.shared.cdi.QueryExtension;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-shared
 * <p>
 * Processor for query scripts that execute JSON expressions. The JSON expression is mainly used to
 * execute fetch query condition judgment and injection processing.
 *
 * @author bingo 下午3:00:31
 */
@Singleton
public class JsonExpressionScriptProcessor extends AbstractScriptProcessor {

  public static final String FILTER_KEY = "filter";
  public static final String SINGLE_KEY = "single";
  public static final String PROJECTION_KEY = "projection";
  public static final String PROJECTION_RENAME_KEY = "rename";
  public static final String PROJECTION_TYPE_KEY = "type";
  public static final String PROJECTION_EVAL_KEY = "eval";

  public static final String PARENT_RESULT_VAR_PREFIX =
      RESULT_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int PARENT_RESULT_VAR_PREFIX_LEN = PARENT_RESULT_VAR_PREFIX.length();
  public static final String FETCH_RESULT_VAR_PREFIX =
      FETCHED_RESULT_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int FETCH_RESULT_VAR_PREFIX_LEN = FETCH_RESULT_VAR_PREFIX.length();
  public static final String PARAMETER_VAR_PREFIX =
      PARAMETER_FUNC_PARAMETER_NAME + Names.NAME_SPACE_SEPARATORS;
  public static final int PARAMETER_VAR_PREFIX_LEN = PARAMETER_VAR_PREFIX.length();

  protected static final Logger logger =
      Logger.getLogger(JsonExpressionScriptProcessor.class.getCanonicalName());

  protected static final Map<String, Function<ParameterAndResultPair, Object>> injFuns =
      new ConcurrentHashMap<>();
  protected static final Map<String, Function<ParameterAndResult, Object>> preFuns =
      new ConcurrentHashMap<>();
  protected static final List<FunctionResolver> functionResolvers =
      SimpleParser.resolveFunction().collect(Collectors.toList());

  @Inject
  protected QueryObjectMapper mapper;

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion) {
    injFuns.clear();
    preFuns.clear();
    if (QueryExtension.verifyDeployment) {
      logger.info("Start json expression query scripts pre-compiling.");
      int cs = resolveAll(queries, initializedVersion);
      logger.info("Complete " + cs + " json expression query scripts pre-compiling.");
    }
  }

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
      return preFuns.computeIfAbsent(script.getId(), k -> createPreFetchFuns(fetchQuery, script));
    }
    return null;
  }

  @Override
  public boolean supports(Script script) {
    return script != null && script.getType() == ScriptType.JSE;
  }

  /**
   * Returns a fetch result inject function
   *
   * @param fetchQuery a fetch query to build the injection function
   * @param script the injection DSL function script
   */
  protected Function<ParameterAndResultPair, Object> createInjectFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Pair<Node<Boolean>, Projector> eval = resolveInjectScript(code);
    final Node<Boolean> filter = eval.left();
    final Projector projector = eval.right();
    return p -> injectFetchResult(p, fetchQuery, filter, projector);
  }

  /**
   * Returns a fetch query precondition function, use for whether to fetch deciding.
   *
   * @param fetchQuery a fetch query
   * @param script the precondition DSL function script
   */
  @SuppressWarnings("unchecked")
  protected Function<ParameterAndResult, Object> createPreFetchFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Node<Boolean> ast = (Node<Boolean>) SimpleParser.parse(code, MyASTNodeBuilder.INST);
    return p -> {
      Map<Object, Object> r = (Map<Object, Object>) p.result;
      MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
      return ast.getValue(evalCtx.linkParentResult(r));
    };
  }

  /**
   * Inject a fetched query results to parent query results.
   *
   * @param p current query parameter and parent query results and fetched query results
   * @param fetchQuery a fetch query
   * @param filter an injection filter, used to determine which fetch results can be used for
   *        injection of the current parent result
   * @param projector a projector used to handle injection
   */
  protected Object injectFetchResult(ParameterAndResultPair p, FetchQuery fetchQuery,
      final Node<Boolean> filter, final Projector projector) {
    List<Map<Object, Object>> parentResults = forceCast(p.parentResult);
    List<Map<Object, Object>> fetchResults = forceCast(p.fetchedResult);
    MyEvaluationContext evalCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
    for (Map<Object, Object> r : parentResults) {
      // filter the fetched results which can be used for injecting
      List<Object> injectResults = new ArrayList<>();
      if (filter == null) {
        if (!fetchQuery.isMultiRecords()) {
          injectResults.add(fetchResults.get(0));
        } else {
          injectResults.addAll(fetchResults);
        }
      } else {
        for (Map<Object, Object> fr : fetchResults) {
          if (filter.getValue(evalCtx.linkResults(r, fr))) {
            injectResults.add(fr);
            if (!fetchQuery.isMultiRecords()) {
              break;
            }
          }
        }
      }
      // process the filtered injection results: extract->DSL evaluation->type conversion->rename
      if (projector != null && !injectResults.isEmpty()) {
        injectResults = projector.apply(injectResults, evalCtx, mapper);
      }
      // inject the processed and fetched results to parent result
      if (isNotEmpty(fetchQuery.getInjectPropertyNamePath())) {
        // inject with the property name path
        if (fetchQuery.isMultiRecords()) {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(), injectResults);
        } else {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(),
              isNotEmpty(injectResults) ? injectResults.get(0) : null);
        }
      } else {
        // inject without the property name path
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
  }

  /**
   * Resolves a {@link Projector} from given projection maps and options
   *
   * @param projectionMap a projection maps which is extracted from injection script.
   * @param single indicates whether is single value projection or not
   * @see Projector
   */
  @SuppressWarnings("unchecked")
  protected Projector resolveInjectProjector(Map<String, Object> projectionMap, boolean single) {
    Set<Mapping> mappings = new LinkedHashSet<>();
    if (projectionMap != null) {
      projectionMap.forEach((k, v) -> {
        if (k != null && v != null) {
          String[] keyPath = splitNameSpace(k, true, false);
          if (keyPath.length > 0) {
            if (v instanceof Map<?, ?> vm) {
              // parse conversion target type
              String typeName = getMapString(vm, PROJECTION_TYPE_KEY);
              Class<?> type = typeName != null ? asClass(typeName) : null;
              // parse evaluation DSL
              Object evalScript = getMapObject(vm, PROJECTION_EVAL_KEY);
              Node<Object> evalNode = null;
              if (evalScript instanceof Map map) {
                evalNode = forceCast(SimpleParser.parse(map, MyASTNodeBuilder.INST));
              } else if (evalScript != null) {
                throw new QueryRuntimeException(
                    "The %s projection error, 'eval' can only be a Map!", k);
              }
              // parse injection rename
              Object renamePaths = vm.get(PROJECTION_RENAME_KEY);
              if (renamePaths != null && evalNode != null) {
                throw new QueryRuntimeException(
                    "The %s projection error, 'rename' and 'eval' can't be used together!", k);
              }
              renamePaths = renamePaths == null ? k : renamePaths;
              List<String[]> renames = null;
              if (renamePaths instanceof String rps && isNotBlank(rps)) {
                // for single injection
                renames = singletonList(splitNameSpace(rps, true, false));
              } else if ((renamePaths instanceof Collection<?> rpc) && !rpc.isEmpty()) {
                // for multiple injections
                renames = new ArrayList<>(rpc.size());
                for (Object ro : rpc) {
                  if (ro != null) {
                    renames.add(splitNameSpace(ro.toString(), true, false));
                  }
                }
              }
              mappings.add(new Mapping(keyPath, renames, evalNode, type));
            } else if (toBoolean(v)) {
              mappings.add(new Mapping(keyPath, singletonList(keyPath), null, null));
            }
          }
        }
      });
    }
    if (isEmpty(mappings) && !single) {
      throw new QueryRuntimeException("The projection can't empty!");
    } else if (single && (mappings.size() > 1
        || (mappings.size() == 1 && mappings.iterator().next().injectPaths.size() > 1))) {
      throw new QueryRuntimeException(
          "In a single case, the projection can't contain multi fields mapping!");
    }
    return new Projector(mappings, single);
  }

  /**
   * Resolves an injection processing by given script code
   * <p>
   * The returns injection processing is a pair, the left value of the pair is a script used for
   * filtering and the right value of the pair is a projector used to process the filtered injection
   * result.
   *
   * @param code the script code to be resolved
   */
  protected Pair<Node<Boolean>, Projector> resolveInjectScript(String code) {
    final Map<String, Object> root = Jsons.fromString(code);
    Map<String, Object> filterMap = getMapMap(root, FILTER_KEY);
    Map<String, Object> projectionMap = getMapMap(root, PROJECTION_KEY);
    boolean single = getMapBoolean(root, SINGLE_KEY, Boolean.FALSE);
    Node<Boolean> filter = null;
    Projector projector = null;
    if (filterMap != null) {
      filter = forceCast(SimpleParser.parse(filterMap, MyASTNodeBuilder.INST));
    }
    if (projectionMap != null || single) {
      projector = resolveInjectProjector(projectionMap, single);
    }
    if (filter == null && projector == null) {
      filter = forceCast(SimpleParser.parse(root, MyASTNodeBuilder.INST));
    }
    return Pair.of(filter, projector);
  }

  /**
   * corant-modules-query-shared
   * <p>
   * A projection class for processing filtered results that match the injection criteria. It will
   * do in order according to the projection configuration of each field: extraction and then
   * complex evaluation(optional) and then type conversion(optional) and then rename the
   * field(optional).
   * <p>
   * Note: Rename process and complex evaluation can't be used together.
   *
   * @author bingo 下午3:17:15
   */
  protected static class Projector {

    final Collection<Mapping> mappings;
    final boolean single;

    Projector(Collection<Mapping> mappings, boolean single) {
      this.mappings = mappings == null ? emptyList() : unmodifiableCollection(mappings);
      this.single = single;
    }

    List<Object> apply(List<Object> fetchResults, MyEvaluationContext evalCtx,
        QueryObjectMapper mapper) {
      List<Object> results = new ArrayList<>();
      if (single) {
        if (mappings.isEmpty()) {
          results.addAll(fetchResults);
        } else {
          final Mapping mapping = mappings.iterator().next();
          for (Object fetchResult : fetchResults) {
            Object extracted = process(fetchResult, mapping, evalCtx, mapper);
            results.add(extracted);
          }
        }
      } else {
        for (Object fetchResult : fetchResults) {
          Map<Object, Object> result = new LinkedHashMap<>();
          for (Mapping mapping : mappings) {
            Object extracted = process(fetchResult, mapping, evalCtx, mapper);
            for (String[] injectPath : mapping.injectPaths) {
              mapper.putMappedValue(result, injectPath, extracted);
            }
          }
          results.add(result);
        }
      }
      return results;
    }

    @SuppressWarnings("unchecked")
    Object process(Object fetchResult, Mapping mapping, MyEvaluationContext evalCtx,
        QueryObjectMapper mapper) {
      Object extracted;
      if (mapping.evalNode != null) {
        // handle complex evaluation if necessary
        extracted =
            mapping.evalNode.getValue(evalCtx.linkFetchResult((Map<Object, Object>) fetchResult));
      } else {
        // extract the field value from fetched result
        extracted = mapper.getMappedValue(fetchResult, mapping.extractPath);
      }
      // convert the field value to target type if necessary
      if (mapping.type != null) {
        extracted = toObject(extracted, mapping.type);
      }
      return extracted;
    }
  }

  /**
   * corant-modules-query-shared
   * <p>
   * A single field(or property) projection configuration. Contains the field extract from and
   * inject to paths, a target type used for value conversion, an evaluation script used for complex
   * solution.
   * <p>
   * The evaluation script is used to solve a result according to the context
   *
   * @author bingo 下午3:13:21
   */
  static class Mapping {
    final String[] extractPath;
    final List<String[]> injectPaths;
    final Class<?> type;
    final Node<Object> evalNode;

    Mapping(String[] extractPath, List<String[]> injectPaths, Node<Object> evalNode,
        Class<?> type) {
      this.extractPath = extractPath;
      this.injectPaths = injectPaths;
      this.evalNode = evalNode;
      this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || (getClass() != obj.getClass())) {
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
   * <p>
   * An AST variable node containing the name path used for expression variable extraction. The
   * expression variables may a parent query result or a fetch query result or a query parameter or
   * a certain single field value.
   *
   * @author bingo 下午3:19:55
   */
  static class MyASTVariableNode extends ASTDefaultVariableNode {

    private final Object[] namePath;

    MyASTVariableNode(String name) {
      super(name);
      if (name.startsWith(PARENT_RESULT_VAR_PREFIX)) {
        namePath = splitNameSpace(name.substring(PARENT_RESULT_VAR_PREFIX_LEN), true, false);
      } else if (name.startsWith(FETCH_RESULT_VAR_PREFIX)) {
        namePath = splitNameSpace(name.substring(FETCH_RESULT_VAR_PREFIX_LEN), true, false);
      } else if (name.startsWith(PARAMETER_VAR_PREFIX)) {
        namePath = splitNameSpace(name.substring(PARAMETER_VAR_PREFIX_LEN), true, false);
      } else {
        throw new NotSupportedException(
            "Dynamic query json expression variable with name [%s] is not supported!", name);
      }
    }

    Object[] getNamePath() {
      return namePath;
    }
  }

  /**
   * corant-modules-query-shared
   * <p>
   * An evaluation context used for expression variables and function evaluation. The context may
   * contain current parent query result, current fetch query result, current query parameter or a
   * certain field value etc.
   * <p>
   * Note: This class is not thread-safe.
   *
   * @author bingo 下午3:19:59
   */
  static class MyEvaluationContext implements EvaluationContext {

    Map<Object, Object> parentResult;
    Map<Object, Object> fetchResult;
    Map<Object, Object> queryParameterMap;
    boolean queryParamMapResolved;
    final Object queryParameter;
    final QueryObjectMapper objectMapper;
    final List<FunctionResolver> functionResolvers;

    MyEvaluationContext(QueryObjectMapper objectMapper, Object queryParameter,
        List<FunctionResolver> functionResolvers) {
      this.objectMapper = objectMapper;
      this.queryParameter = queryParameter;
      this.functionResolvers = functionResolvers;
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
        // handle parent result variable: @r.[field name]
        return objectMapper.getMappedValue(parentResult, myNode.getNamePath());
      } else if (myNode.getName().startsWith(FETCH_RESULT_VAR_PREFIX)) {
        // handle fetch result variable: @fr.[field name]
        return objectMapper.getMappedValue(fetchResult, myNode.getNamePath());
      } else {
        // handle query parameter: @p.criteria.[criterion name] or @p.context.[context key]
        if (!queryParamMapResolved) {
          queryParameterMap = objectMapper.toObject(queryParameter, Map.class);
          queryParamMapResolved = true;
        }
        return objectMapper.getMappedValue(queryParameterMap, myNode.getNamePath());
      }
    }

    MyEvaluationContext linkFetchResult(Map<Object, Object> fetchResult) {
      this.fetchResult = fetchResult;
      return this;
    }

    MyEvaluationContext linkParentResult(Map<Object, Object> parentResult) {
      this.parentResult = parentResult;
      return this;
    }

    MyEvaluationContext linkResults(Map<Object, Object> parentResult,
        Map<Object, Object> fetchResult) {
      this.parentResult = parentResult;
      this.fetchResult = fetchResult;
      return this;
    }
  }
}
