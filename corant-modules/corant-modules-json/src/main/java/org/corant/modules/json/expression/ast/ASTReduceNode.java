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
import org.corant.modules.json.expression.EvaluationContext.BindableEvaluationContext;
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

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.REDUCE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getValue(EvaluationContext ctx) {
    Node<?> inputNode = children.get(0);
    final Object input = inputNode.getValue(ctx);
    final BindableEvaluationContext useCtx = new BindableEvaluationContext(ctx);

    if (children.size() == 3) {
      // only accumulator;
      Node<?> accumulatorNamesNode = children.get(1);
      Node<?> accumulatorNode = children.get(2);
      String[] accumulatorNames = ASTNode.variableNamesOf(accumulatorNamesNode, useCtx);
      final BinaryOperator<Object> accumulatorFunc = (u, t) -> accumulatorNode
          .getValue(useCtx.unbindAll().bind(accumulatorNames[0], t).bind(accumulatorNames[1], u));
      if (input instanceof Object[] array) {
        return Arrays.stream(array).reduce(accumulatorFunc).orElse(null);
      } else if (input instanceof Iterable itr) {
        return Streams.streamOf(itr).reduce(accumulatorFunc).orElse(null);
      } else {
        return Arrays.stream(new Object[] {input}).reduce(accumulatorFunc).orElse(null);
      }
    } else if (children.size() == 4) {
      // identity & accumulator
      Node<?> identityNode = children.get(1);
      Node<?> accumulatorNamesNode = children.get(2);
      Node<?> accumulatorNode = children.get(3);
      String[] accumulatorNames = ASTNode.variableNamesOf(accumulatorNamesNode, useCtx);
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
    // the combiner is only used in parallel stream, so we use an empty bi-consumer
    // else if (children.size() == 6) {
    // // identity & accumulator & combiner
    // Node<?> identityNode = children.get(1);
    // Node<?> accumulatorNamesNode = children.get(2);
    // Node<?> accumulatorNode = children.get(3);
    // Node<?> combinerNamesNode = children.get(4);
    // Node<?> combinerNode = children.get(5);
    // String[] accumulatorNames = ASTNode.variableNamesOf(accumulatorNamesNode, useCtx);
    // final BiFunction<Object, Object, Object> accumulatorFunc = (u, t) -> accumulatorNode
    // .getValue(useCtx.unbindAll().bind(accumulatorNames[0], u).bind(accumulatorNames[1], t));
    // String[] combinerNames = ASTNode.variableNamesOf(combinerNamesNode, useCtx);
    // final BinaryOperator<Object> combinerFunc = (u1, u2) -> combinerNode
    // .getValue(useCtx.unbindAll().bind(combinerNames[0], u1).bind(combinerNames[1], u2));
    //
    // if (input instanceof Object[] array) {
    // return Arrays.asList(array).parallelStream().reduce(identityNode.getValue(useCtx),
    // accumulatorFunc, combinerFunc);
    // } else if (input instanceof Iterable itr) {
    // return listOf(itr).parallelStream().reduce(identityNode.getValue(useCtx), accumulatorFunc,
    // combinerFunc);
    // } else {
    // return Arrays.stream(new Object[] {input}).reduce(identityNode.getValue(useCtx),
    // accumulatorFunc, combinerFunc);
    // }
    // }
    throw new NotSupportedException();
  }

}
