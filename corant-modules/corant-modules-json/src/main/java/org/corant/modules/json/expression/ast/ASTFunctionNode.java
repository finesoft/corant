/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Strings.strip;
import org.corant.modules.json.expression.EvaluationContext;

/**
 * corant-modules-json
 *
 * @author bingo 下午10:24:55
 */
public interface ASTFunctionNode extends ASTNode<Object> {

  String getName();

  /**
   * corant-modules-json
   *
   * @author bingo 下午2:48:46
   */
  class ASTDefaultFunctionNode extends AbstractASTNode<Object> implements ASTFunctionNode {

    protected final String name;

    public ASTDefaultFunctionNode(String name) {
      this.name = shouldNotBlank(strip(name));
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public ASTNodeType getType() {
      return ASTNodeType.FUN;
    }

    @Override
    public Object getValue(EvaluationContext ctx) {
      return ctx.resolveFunction(this).apply(children.stream().map(c -> c.getValue(ctx)).toArray());
    }
  }

}
