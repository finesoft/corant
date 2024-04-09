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
 * @author bingo 20:49:30
 */
public class ASTNVLNode extends AbstractASTNode<Object> {

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.NVL;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    Object value = children.get(0).getValue(ctx);
    if (children.size() == 2) {
      return value == null ? children.get(1).getValue(ctx) : value;
    } else {
      return value == null ? children.get(2).getValue(ctx) : children.get(1).getValue(ctx);
    }
  }

}
