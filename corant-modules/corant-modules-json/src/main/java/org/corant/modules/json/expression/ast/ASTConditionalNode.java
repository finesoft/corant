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

import static org.corant.shared.util.Conversions.toBoolean;
import java.util.ArrayList;
import java.util.List;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:22:18
 */
public class ASTConditionalNode implements ASTNode<Object> {

  protected List<ASTNode<?>> children = new ArrayList<>();

  @Override
  public boolean addChild(Node<?> child) {
    return children.add((ASTNode<?>) child);
  }

  @Override
  public List<? extends Node<?>> getChildren() {
    return children;
  }

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.CONDITIONAL;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    Node<?> condition = children.get(0);
    if (children.size() == 2) {
      return toBoolean(condition.getValue(ctx)) ? children.get(1).getValue(ctx) : null;
    } else {
      return toBoolean(condition.getValue(ctx)) ? children.get(1).getValue(ctx)
          : children.get(2).getValue(ctx);
    }
  }
}
