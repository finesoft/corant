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
import java.util.function.BiConsumer;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Streams;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTCollectNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected Node<?> supplierNode;
  protected ASTDeclarationNode accumulatorNamesNode;
  protected Node<?> accumulatorNode;
  protected String[] accumulatorNames;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.COLLECT;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
    final BiConsumer<Object, Object> accumulatorFunc = (u, t) -> accumulatorNode
        .getValue(useCtx.unbindAll().bind(accumulatorNames[0], u).bind(accumulatorNames[1], t));
    // the combiner is only used in parallel stream, so we use an empty bi-consumer
    final BiConsumer<Object, Object> combinerFunc = Functions.emptyBiConsumer();
    if (input instanceof Object[] array) {
      return Arrays.stream(array).collect(() -> supplierNode.getValue(useCtx), accumulatorFunc,
          combinerFunc);
    } else if (input instanceof Iterable<?> itr) {
      return Streams.streamOf(itr).collect(() -> supplierNode.getValue(useCtx), accumulatorFunc,
          combinerFunc);
    } else {
      return Arrays.stream(new Object[] {input}).collect(() -> supplierNode.getValue(useCtx),
          accumulatorFunc, combinerFunc);
    }
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    // supplier & accumulator & combiner(fake)
    supplierNode = children.get(1);
    accumulatorNamesNode = (ASTDeclarationNode) children.get(2);
    accumulatorNode = children.get(3);
    accumulatorNames = accumulatorNamesNode.getVariableNames();
  }

}
