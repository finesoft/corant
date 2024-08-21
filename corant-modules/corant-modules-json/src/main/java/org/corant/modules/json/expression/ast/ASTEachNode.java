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

import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;

/**
 * corant-modules-json
 * <p>
 * FIXME uncompleted yet!
 *
 * @author bingo 下午2:22:18
 */
public class ASTEachNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected ASTDeclarationNode eachNameNode;
  protected Node<?> subroutineNode;
  protected Node<?> breakNode;
  protected String eachName;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.EACH;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
    if (input instanceof Object[] array) {
      for (Object element : array) {
        subroutineNode.getValue(useCtx.bind(eachName, element));
      }
    } else if (input instanceof Iterable<?> itr) {
      for (Object mo : itr) {
        subroutineNode.getValue(useCtx.bind(eachName, mo));
      }
    } else {
      subroutineNode.getValue(useCtx.bind(eachName, input));
    }
    return null;
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    eachNameNode = (ASTDeclarationNode) children.get(1);
    subroutineNode = children.get(2);
    eachName = eachNameNode.getVariableNames()[0];
  }

}
