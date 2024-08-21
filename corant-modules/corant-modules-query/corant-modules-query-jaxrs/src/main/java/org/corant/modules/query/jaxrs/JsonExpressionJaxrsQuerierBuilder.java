/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import static org.corant.shared.util.Maps.getMapKeyPathValue;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.json.ObjectMappers;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.SimpleParser;
import org.corant.modules.json.expression.ast.ASTFunctionNode;
import org.corant.modules.json.expression.ast.ASTVariableNode;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier.JaxrsQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:55:32
 */
@Experimental
public class JsonExpressionJaxrsQuerierBuilder
    extends AbstractNamedQuerierBuilder<JaxrsNamedQuerier> {

  protected final Client client;
  protected final JaxrsNamedQueryClientConfig clientConfig;
  protected final Node<?> execution;
  protected final List<FunctionResolver> functionResolvers;

  protected JsonExpressionJaxrsQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler, Client client,
      JaxrsNamedQueryClientConfig clientConfig) {
    super(query, queryHandler, fetchQueryHandler);
    this.client = client;
    this.clientConfig = clientConfig;
    execution = SimpleParser.parse(query.getScript().getCode());
    functionResolvers = SimpleParser.resolveFunction().toList();
  }

  @Override
  public DefaultJaxrsNamedQuerier build(QueryParameter queryParameter) {
    WebTarget target = client.target(clientConfig.getRoot());
    Map<?, ?> result = forceCast(execution.getValue(new JaxrsEvaluationContext(
        getQueryHandler().getObjectMapper().mapOf(queryParameter, true), functionResolvers)));
    JaxrsQueryParameter parameter =
        getQueryHandler().getObjectMapper().toObject(result, JaxrsQueryParameter.class);
    parameter.postConstruct();
    return new DefaultJaxrsNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), clientConfig, target, parameter);
  }

  /**
   * corant-modules-query-jaxrs
   *
   * @author bingo 15:25:15
   */
  public static class JaxrsEvaluationContext implements EvaluationContext {

    protected final Map<String, Object> bindings;
    protected final List<FunctionResolver> functionResolvers;

    public JaxrsEvaluationContext(Map<String, Object> bindings,
        List<FunctionResolver> functionResolvers) {
      this.bindings = bindings;
      this.functionResolvers = functionResolvers;
    }

    @Override
    public Function<Object[], Object> resolveFunction(Node<?> node) {
      ASTFunctionNode funcNode = (ASTFunctionNode) node;
      return functionResolvers.stream().filter(fr -> fr.supports(funcNode.getName()))
          .min(Sortable::compare).orElseThrow(NotSupportedException::new)
          .resolve(funcNode.getName());
    }

    @Override
    public Object resolveVariableValue(Node<?> node) {
      if (node instanceof ASTVariableNode varNode) {
        String[] varNamespace = varNode.getNamespace();
        if (bindings.containsKey(varNamespace[0])) {
          Object boundValue = bindings.get(varNamespace[0]);
          if (varNamespace.length == 1) {
            return boundValue;
          } else if (boundValue instanceof Map<?, ?> boundMap) {
            return getMapKeyPathValue(boundMap,
                Arrays.copyOfRange(varNamespace, 1, varNamespace.length), false);
          } else if (boundValue != null) {
            return getMapKeyPathValue(ObjectMappers.toMap(boundValue),
                Arrays.copyOfRange(varNamespace, 1, varNamespace.length), false);
          } else {
            return null;
          }
        }
      }
      throw new NotSupportedException();
    }

  }
}
