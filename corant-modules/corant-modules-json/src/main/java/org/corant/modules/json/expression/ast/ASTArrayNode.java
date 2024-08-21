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

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;

/**
 * corant-modules-json
 *
 * @author bingo 18:19:29
 */
public class ASTArrayNode extends AbstractASTNode<Object> {

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.ARRAY;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    if (children.isEmpty()) {
      return new ArrayList<>();
    }
    return children.stream().map(c -> c.getValue(ctx)).collect(Collectors.toList());
  }

}
