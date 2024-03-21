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

import static org.corant.shared.util.Lists.subList;
import java.util.List;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTSubroutineNode extends AbstractASTNode<Object> {

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.SUBROUTINE;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Node<?> xNode = children.get(0);
    String varName = null;
    if (xNode instanceof ASTValueNode avn && avn.value() instanceof String anvs) {
      String[] ns = ASTNode.parseVariableNames(anvs);
      if (ns.length > 0) {
        varName = ns[0];
      }
    }
    if (varName != null) {
      final List<ASTNode<?>> outputNodes = subList(children, 1, children.size());
      SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
      Object val = null;
      for (ASTNode<?> outputNode : outputNodes) {
        val = outputNode.getValue(useCtx.bind(varName, val));
      }
      return val;
    } else {
      Object val = null;
      for (ASTNode<?> outputNode : children) {
        val = outputNode.getValue(ctx);
      }
      return val;
    }
  }

}
