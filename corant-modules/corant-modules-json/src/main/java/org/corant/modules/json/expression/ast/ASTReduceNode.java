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

import java.util.Arrays;
import java.util.function.BinaryOperator;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Streams;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTReduceNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected Node<?> identityNode;
  protected ASTDeclarationNode accumulatorNamesNode;
  protected Node<?> accumulatorNode;
  protected String[] accumulatorNames;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.REDUCE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);

    if (identityNode == null) {
      // only accumulator;
      final BinaryOperator<Object> accumulatorFunc = (u, t) -> accumulatorNode
          .getValue(useCtx.unbindAll().bind(accumulatorNames[0], t).bind(accumulatorNames[1], u));
      if (input instanceof Object[] array) {
        return Arrays.stream(array).reduce(accumulatorFunc).orElse(null);
      } else if (input instanceof Iterable itr) {
        return Streams.streamOf(itr).reduce(accumulatorFunc).orElse(null);
      } else {
        return Arrays.stream(new Object[] {input}).reduce(accumulatorFunc).orElse(null);
      }
    } else {
      // identity & accumulator
      final BinaryOperator<Object> accumulatorFunc = (u, t) -> accumulatorNode
          .getValue(useCtx.unbindAll().bind(accumulatorNames[0], u).bind(accumulatorNames[1], t));
      if (input instanceof Object[] array) {
        return Arrays.stream(array).reduce(identityNode.getValue(useCtx), accumulatorFunc);
      } else if (input instanceof Iterable itr) {
        return Streams.streamOf(itr).reduce(identityNode.getValue(useCtx), accumulatorFunc);
      } else {
        return Arrays.stream(new Object[] {input}).reduce(identityNode.getValue(useCtx),
            accumulatorFunc);
      }
    }
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    if (children.size() == 3) {
      // only accumulator;
      accumulatorNamesNode = (ASTDeclarationNode) children.get(1);
      accumulatorNames = accumulatorNamesNode.getVariableNames();
      accumulatorNode = children.get(2);
    } else if (children.size() == 4) {
      // identity & accumulator
      identityNode = children.get(1);
      accumulatorNamesNode = (ASTDeclarationNode) children.get(2);
      accumulatorNames = accumulatorNamesNode.getVariableNames();
      accumulatorNode = children.get(3);
    } else {
      throw new NotSupportedException();
    }
  }
}
