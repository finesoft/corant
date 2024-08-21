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

import static org.corant.shared.util.Streams.streamOf;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.SubEvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTFilterNode extends AbstractASTNode<Object> {

  protected Node<?> inputNode;
  protected ASTDeclarationNode inputElementVarNameNode;
  protected Node<?> filterNode;
  protected String varName;

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.FILTER;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    final Object input = inputNode.getValue(ctx);
    final SubEvaluationContext useCtx = new SubEvaluationContext(ctx);
    if (input instanceof Object[] array) {
      return Arrays.stream(array)
          .filter(fo -> (Boolean) filterNode.getValue(useCtx.bind(varName, fo))).toArray();
    } else if (input instanceof Iterable<?> itr) {
      return streamOf(itr).filter(fo -> (Boolean) filterNode.getValue(useCtx.bind(varName, fo)))
          .collect(Collectors.toList());
    } else if (input != null) {
      return (Boolean) filterNode.getValue(useCtx.bind(varName, input)) ? input : null;
    } else {
      return null;
    }
  }

  @Override
  public void postConstruct() {
    super.postConstruct();
    inputNode = children.get(0);
    inputElementVarNameNode = (ASTDeclarationNode) children.get(1);
    filterNode = children.get(2);
    varName = inputElementVarNameNode.getVariableNames()[0];
  }

}
