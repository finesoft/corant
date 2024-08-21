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

import java.lang.reflect.Array;
import java.util.ArrayList;
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
public class ASTMapNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected ASTDeclarationNode inputNameNode;
  protected Node<?> outputNode;
  protected String inputName;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.MAP;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
    if (input instanceof Object[] array) {
      Object[] output =
          (Object[]) Array.newInstance(input.getClass().componentType(), array.length);
      for (int i = 0; i < array.length; i++) {
        output[i] = outputNode.getValue(useCtx.bind(inputName, array[i]));
      }
      return output;
    } else if (input instanceof Iterable<?> itr) {
      List<Object> output = new ArrayList<>();
      for (Object mo : itr) {
        output.add(outputNode.getValue(useCtx.bind(inputName, mo)));
      }
      return output;
    } else {
      return outputNode.getValue(useCtx.bind(inputName, input));
    }
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    inputNameNode = (ASTDeclarationNode) children.get(1);
    outputNode = children.get(2);
    inputName = inputNameNode.getVariableNames()[0];
  }

}
