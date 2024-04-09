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
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;

/**
 * corant-modules-json
 *
 * @author bingo 18:17:19
 */
public class ASTNullCheckNode extends AbstractASTNode<Boolean> {

  protected final boolean nonNull;

  public ASTNullCheckNode(boolean nonNull) {
    this.nonNull = nonNull;
  }

  @Override
  public ASTNodeType getType() {
    return nonNull ? ASTNodeType.NON_NULL : ASTNodeType.IS_NULL;
  }

  @Override
  public Boolean getValue(EvaluationContext ctx) {
    Object value = children.get(0).getValue(ctx);
    if (nonNull) {
      return value != null;
    }
    return value == null;
  }

}
