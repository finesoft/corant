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

import static org.corant.shared.util.Conversions.toInteger;
import java.util.Arrays;
import java.util.Comparator;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;
import org.corant.shared.util.Streams;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTMinNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected ASTDeclarationNode sortableNamesNode;
  protected Node<?> sorterNode;
  protected String[] sortableNames;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.MIN;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
    final Comparator<Object> comparator = (t1, t2) -> toInteger(sorterNode
        .getValue(useCtx.unbindAll().bind(sortableNames[0], t1).bind(sortableNames[1], t2)));
    if (input instanceof Object[] array) {
      return Arrays.stream(array).min(comparator).orElse(null);
    } else if (input instanceof Iterable<?> itr) {
      return Streams.streamOf(itr).min(comparator).orElse(null);
    } else {
      return input;
    }
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    sortableNamesNode = (ASTDeclarationNode) children.get(1);
    sorterNode = children.get(2);
    sortableNames = sortableNamesNode.getVariableNames();
  }
}
