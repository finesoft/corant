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
package org.corant.modules.json.expression.ast;

import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Streams.streamOf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.BindableEvaluationContext;
import org.corant.modules.json.expression.Node;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTFilterNode implements ASTNode<Object> {

  protected final List<ASTNode<?>> children = new ArrayList<>();

  @Override
  public boolean addChild(Node<?> child) {
    return children.add((ASTNode<?>) child);
  }

  @Override
  public List<? extends Node<?>> getChildren() {
    return children;
  }

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.FILTER;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Node<?> inputNode = children.get(0);
    final Node<?> inputElementVarNameNode = children.get(1);
    final Node<?> filterNode = children.get(2);
    Object input = inputNode.getValue(ctx);
    String varName = ASTNode.variableNamesOf(inputElementVarNameNode, ctx)[0];

    BindableEvaluationContext useCtx = new BindableEvaluationContext(ctx);
    if (input instanceof Object[] array) {
      return Arrays.stream(array)
          .filter(fo -> toBoolean(filterNode.getValue(useCtx.bind(varName, fo))))
          .collect(Collectors.toList());
    } else if (input instanceof Iterable<?> itr) {
      return streamOf(itr).filter(fo -> toBoolean(filterNode.getValue(useCtx.bind(varName, fo))))
          .collect(Collectors.toList());
    } else if (input != null) {
      return toBoolean(filterNode.getValue(useCtx.bind(varName, input))) ? input : null;
    } else {
      return null;
    }
  }

}
