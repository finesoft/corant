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

import static org.corant.shared.normal.Names.splitNameSpace;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Strings.strip;
import java.util.Arrays;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;

/**
 * corant-modules-json
 *
 * @author bingo 下午10:24:55
 */
public interface ASTVariableNode extends ASTNode<Object> {

  String getName();

  default String[] getNamespace() {
    String[] array = splitNameSpace(getName(), true, false);
    return Arrays.copyOf(array, array.length);
  }

  class ASTDefaultVariableNode implements ASTVariableNode {

    protected ASTNode<?> parent;
    protected final String name;
    protected final String[] namespace;

    public ASTDefaultVariableNode(String name) {
      this.name = shouldNotBlank(strip(name));
      namespace = splitNameSpace(this.name, true, false);
      shouldBeTrue(namespace.length > 0);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String[] getNamespace() {
      return Arrays.copyOf(namespace, namespace.length);
    }

    @Override
    public ASTNode<?> getParent() {
      return parent;
    }

    @Override
    public ASTNodeType getType() {
      return ASTNodeType.VAR;
    }

    @Override
    public Object getValue(EvaluationContext ctx) {
      return ctx.resolveVariableValue(this);
    }

    @Override
    public void setParent(Node<?> parent) {
      this.parent = (ASTNode<?>) parent;
    }

  }
}
