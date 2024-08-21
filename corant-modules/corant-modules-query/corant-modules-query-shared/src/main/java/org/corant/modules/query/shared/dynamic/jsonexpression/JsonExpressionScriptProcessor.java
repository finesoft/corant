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

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static org.corant.shared.normal.Names.splitNameSpace;
import static org.corant.shared.util.Assertions.shouldBeNull;
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
import org.corant.modules.json.ObjectMappers;
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
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Texts;
import com.fasterxml.jackson.core.JsonProcessingException;
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
  public static final String EVAL_KEY = "eval";

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
      return injFuns.computeIfAbsent(script.getId(), k -> createInjectionFuns(fetchQuery, script));
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
  protected Function<ParameterAndResultPair, Object> createInjectionFuns(FetchQuery fetchQuery,
      Script script) {
    final String code = script.getCode();
    final Pair<Node<Boolean>, InjectionHandler> injectScripts = resolveInjectionScript(code);
    final Node<Boolean> filter = injectScripts.left();
    final InjectionHandler handler = injectScripts.right();
    return p -> injectFetchResult(p, fetchQuery, filter, handler);
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
      return ast.getValue(evalCtx.bindParentResult(r));
    };
  }

  /**
   * Inject a fetched query results to parent query results.
   *
   * @param p current query parameter and parent query results and fetched query results
   * @param fetchQuery a fetch query
   * @param filter an injection filter, used to determine which fetch results can be used for
   *        injection of the current parent result
   * @param handler a handler used to handle injection
   */
  protected Object injectFetchResult(ParameterAndResultPair p, FetchQuery fetchQuery,
      final Node<Boolean> filter, final InjectionHandler handler) {
    List<Map<Object, Object>> parentResults = forceCast(p.parentResult);
    List<Map<Object, Object>> fetchResults = forceCast(p.fetchedResult);
    MyEvaluationContext injectCtx = new MyEvaluationContext(mapper, p.parameter, functionResolvers);
    for (Map<Object, Object> r : parentResults) {
      // filter the fetched results which can be used for injecting
      injectCtx.unbindAllResults().bindParentResult(r);
      List<Map<Object, Object>> injectResults;
      if (filter == null) {
        injectResults = fetchResults;
      } else {
        injectResults = new ArrayList<>();
        for (Map<Object, Object> fr : fetchResults) {
          if (filter.getValue(injectCtx.bindFetchResult(fr))) {
            injectResults.add(fr);
          }
        }
      }
      // since the handled may be a Single type list, we need to create a new list for them
      List<?> handledResults = injectResults;
      // process the filtered injection results: extract->DSL evaluation->type conversion->rename
      if (handler != null && !injectResults.isEmpty()) {
        handledResults = handler.apply(injectResults, injectCtx, mapper);
      }
      // inject the handled results to the parent result
      if (isNotEmpty(fetchQuery.getInjectPropertyNamePath())) {
        // inject with the property name path
        if (fetchQuery.isMultiRecords()) {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(), handledResults);
        } else {
          mapper.putMappedValue(r, fetchQuery.getInjectPropertyNamePath(),
              isNotEmpty(handledResults) ? handledResults.get(0) : null);
        }
      } else {
        // inject without the property name path
        for (Object handledResult : handledResults) {
          if (handledResult != null) {
            Map<Object, Object> handledResultMap = forceCast(handledResult);
            r.putAll(handledResultMap);
            if (!fetchQuery.isMultiRecords()) {
              break;
            }
          }
        }
      }
    }
    return null;
  }

  protected InjectionHandler resolveInjectionHandler(Map<String, Object> projectionMap) {
    Node<Object> evalNode = forceCast(SimpleParser.parse(projectionMap, MyASTNodeBuilder.INST));
    return new Evaluator(evalNode);
  }

  /**
   * Resolves a {@link InjectionHandler} from given projection maps and options
   *
   * @param projectionMap projection maps that are extracted from the injection script.
   * @param single indicates whether is single value projection or not
   * @see InjectionHandler
   */
  protected InjectionHandler resolveInjectionHandler(Map<String, Object> projectionMap,
      boolean single) {
    Set<ProjectionMapping> mappings = new LinkedHashSet<>();
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
              Object evalScript = getMapObject(vm, EVAL_KEY);
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
              mappings.add(new ProjectionMapping(keyPath, renames, evalNode, type));
            } else if (toBoolean(v)) {
              mappings.add(new ProjectionMapping(keyPath, singletonList(keyPath), null, null));
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
   * Resolves injection processing by given script code
   * <p>
   * The return injection processing is a pair, the left value of the pair is a script used for
   * filtering and the right value of the pair is a {@link InjectionHandler} used to process the
   * filtered injection result.
   *
   * @param code the script code to be resolved
   */
  protected Pair<Node<Boolean>, InjectionHandler> resolveInjectionScript(String code) {
    Map<String, Object> root;
    try {
      root = ObjectMappers.mapReader().readValue(code);
    } catch (JsonProcessingException e) {
      if (e.getLocation() != null && code != null) {
        throw new QueryRuntimeException(e,
            format("Injection script syntax error:%n%s", Texts.labelLineAndColumn(code,
                e.getLocation().getLineNr(), e.getLocation().getColumnNr(), null, null)));
      } else {
        throw new QueryRuntimeException(e);
      }
    }
    Map<String, Object> filterMap = getMapMap(root, FILTER_KEY);
    Map<String, Object> projectionMap = getMapMap(root, PROJECTION_KEY);
    boolean single = getMapBoolean(root, SINGLE_KEY, Boolean.FALSE);
    Map<String, Object> evalMap = getMapMap(root, EVAL_KEY);

    Node<Boolean> filter = null;
    InjectionHandler injectionHandler = null;
    if (filterMap != null) {
      filter = forceCast(SimpleParser.parse(filterMap, MyASTNodeBuilder.INST));
    }
    if (projectionMap != null || single) {
      shouldBeNull(evalMap, "Injection script error! root 'eval' can only be used with 'filter'! ");
      injectionHandler = resolveInjectionHandler(projectionMap, single);
    } else if (evalMap != null) {
      injectionHandler = resolveInjectionHandler(evalMap);
    }
    if (filter == null && injectionHandler == null) {
      filter = forceCast(SimpleParser.parse(root, MyASTNodeBuilder.INST));
    }
    return Pair.of(filter, injectionHandler);
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 15:33:14
   */
  public static class Evaluator implements InjectionHandler {

    protected final Node<?> eval;

    protected Evaluator(Node<?> eval) {
      this.eval = eval;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> apply(List<Map<Object, Object>> fetchResults,
        MyEvaluationContext injectionCtx, QueryObjectMapper mapper) {
      Object result = eval.getValue(injectionCtx.bindFetchResults(fetchResults));
      return (List<Object>) result;
    }

  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 15:15:18
   */
  public interface InjectionHandler {
    List<Object> apply(List<Map<Object, Object>> fetchResults, MyEvaluationContext injectionCtx,
        QueryObjectMapper mapper);
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:19:53
   */
  public static class MyASTNodeBuilder implements ASTNodeBuilder {

    public static final MyASTNodeBuilder INST = new MyASTNodeBuilder();

    @Override
    public ASTNode<?> build(Object token) {
      ASTNode<?> node = ASTNodeBuilder.DFLT.build(token);
      if (node instanceof ASTVariableNode) {
        return new MyASTVariableNode(((ASTVariableNode) node).getName(), node.getParent());
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
  public static class MyASTVariableNode extends ASTDefaultVariableNode {

    protected final Object[] namePath;

    MyASTVariableNode(String name, Node<?> parent) {
      super(name);
      setParent(parent);
      namePath = Arrays.copyOfRange(namespace, 1, namespace.length);
    }

    Object[] getNamePath() {
      return namePath;
    }
  }

  /**
   * corant-modules-query-shared
   * <p>
   * An evaluation context used for expression variables and function evaluation. The context may
   * contain a current parent query result, current fetch query result, current query parameter or a
   * certain field value etc.
   * <p>
   * Note: This class is not thread-safe.
   *
   * @author bingo 下午3:19:59
   */
  public static class MyEvaluationContext implements EvaluationContext {

    protected final Object queryParameter;
    protected final QueryObjectMapper objectMapper;
    protected final List<FunctionResolver> functionResolvers;

    protected Map<Object, Object> queryParameterMap;
    protected boolean queryParamMapResolved;
    protected Map<Object, Object> parentResult;
    protected Map<Object, Object> fetchResult;
    protected List<Map<Object, Object>> fetchResults;

    public MyEvaluationContext(QueryObjectMapper objectMapper, Object queryParameter,
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
      String rootName = myNode.getNamespace()[0];
      Object[] namePath = myNode.getNamePath();
      if (RESULT_FUNC_PARAMETER_NAME.equals(rootName)) {
        // handle parent result variable: @r.[field name]
        return objectMapper.getMappedValue(parentResult, namePath);
      } else if (FETCHED_RESULT_FUNC_PARAMETER_NAME.equals(rootName)) {
        // handle fetch result variable: @fr.[field name]
        return objectMapper.getMappedValue(fetchResult, namePath);
      } else if (FETCHED_RESULTS_FUNC_PARAMETER_NAME.equals(rootName)) {
        // handle fetch results variable: @frs
        return fetchResults;
      } else {
        // handle query parameter: @p.criteria.[criterion name] or @p.context.[context key]
        if (!queryParamMapResolved) {
          queryParameterMap = objectMapper.toObject(queryParameter, Map.class);
          queryParamMapResolved = true;
        }
        return objectMapper.getMappedValue(queryParameterMap, namePath);
      }
    }

    protected MyEvaluationContext bindFetchResult(Map<Object, Object> fetchResult) {
      this.fetchResult = fetchResult;
      return this;
    }

    protected MyEvaluationContext bindFetchResults(List<Map<Object, Object>> fetchResults) {
      this.fetchResults = fetchResults;
      return this;
    }

    protected MyEvaluationContext bindParentAndFetchResult(Map<Object, Object> parentResult,
        Map<Object, Object> fetchResult) {
      this.parentResult = parentResult;
      this.fetchResult = fetchResult;
      return this;
    }

    protected MyEvaluationContext bindParentResult(Map<Object, Object> parentResult) {
      this.parentResult = parentResult;
      return this;
    }

    protected MyEvaluationContext unbindAllResults() {
      parentResult = null;
      fetchResult = null;
      fetchResults = null;
      return this;
    }
  }

  /**
   * corant-modules-query-shared
   * <p>
   * A single field(or property) projection configuration. Contains the field extract from and
   * inject to paths, a target type used for value conversion, an evaluation script used for some
   * complex solution.
   * <p>
   * The evaluation script is used to solve a result according to the context
   *
   * @author bingo 下午3:13:21
   */
  public static class ProjectionMapping {
    protected final String[] extractPath;
    protected final List<String[]> injectPaths;
    protected final Class<?> type;
    protected final Node<Object> evalNode;

    protected ProjectionMapping(String[] extractPath, List<String[]> injectPaths,
        Node<Object> evalNode, Class<?> type) {
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
      ProjectionMapping other = (ProjectionMapping) obj;
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
   * <p>
   * A projection class for processing filtered results that match the injection criteria. It will
   * do, in order according to the projection configuration of each field: extraction and then
   * complex evaluation(optional) and then type conversion(optional) and then rename the
   * field(optional).
   * <p>
   * Note: A Rename process and complex evaluation can't be used together.
   *
   * @author bingo 下午3:17:15
   */
  public static class Projector implements InjectionHandler {

    protected final Collection<ProjectionMapping> mappings;
    protected final boolean single;

    public Projector(Collection<ProjectionMapping> mappings, boolean single) {
      this.mappings = mappings == null ? emptyList() : unmodifiableCollection(mappings);
      this.single = single;
    }

    @Override
    public List<Object> apply(List<Map<Object, Object>> fetchResults,
        MyEvaluationContext injectionCtx, QueryObjectMapper mapper) {
      List<Object> results = new ArrayList<>();
      if (single) {
        if (mappings.isEmpty()) {
          results.addAll(fetchResults);
        } else {
          final ProjectionMapping mapping = mappings.iterator().next();
          for (Object fetchResult : fetchResults) {
            Object extracted = process(fetchResult, mapping, injectionCtx, mapper);
            results.add(extracted);
          }
        }
      } else {
        for (Object fetchResult : fetchResults) {
          Map<Object, Object> result = new LinkedHashMap<>();
          for (ProjectionMapping mapping : mappings) {
            Object extracted = process(fetchResult, mapping, injectionCtx, mapper);
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
    Object process(Object fetchResult, ProjectionMapping mapping, MyEvaluationContext evalCtx,
        QueryObjectMapper mapper) {
      Object extracted;
      if (mapping.evalNode != null) {
        // handle complex evaluation if necessary
        extracted =
            mapping.evalNode.getValue(evalCtx.bindFetchResult((Map<Object, Object>) fetchResult));
      } else {
        // extract the field value from the fetched result
        extracted = mapper.getMappedValue(fetchResult, mapping.extractPath);
      }
      // convert the field value to the target type if necessary
      if (mapping.type != null) {
        extracted = toObject(extracted, mapping.type);
      }
      return extracted;
    }
  }
}
