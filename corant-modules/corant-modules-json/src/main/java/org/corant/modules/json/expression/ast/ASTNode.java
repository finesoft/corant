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

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.List;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Strings;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:04:44
 */
public interface ASTNode<T> extends Node<T> {

  static String[] parseVariableNames(String varNames) {
    if (varNames.startsWith("(") && varNames.endsWith(")")) {
      return split(varNames.substring(1, varNames.length() - 1), ",", true, true);
    }
    return Strings.EMPTY_ARRAY;
  }

  static String[] variableNamesOf(Node<?> node, EvaluationContext ctx) {
    return parseVariableNames(node.getValue(ctx).toString());
  }

  default void accept(ASTNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  default boolean addChild(Node<?> child) {
    throw new NotSupportedException();
  }

  @Override
  default List<? extends Node<?>> getChildren() {
    throw new NotSupportedException();
  }

  ASTNodeType getType();

  /**
   * corant-modules-json
   *
   * @author bingo 18:17:14
   */
  abstract class AbstractASTNode<T> implements ASTNode<T> {
    protected ASTNode<?> parent;
    protected final List<ASTNode<?>> children = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public boolean addChild(Node<?> child) {
      shouldInstanceOf(child, ASTNode.class).setParent(this);
      return children.add((ASTNode<?>) child);
    }

    @Override
    public List<? extends Node<?>> getChildren() {
      return children;
    }

    @Override
    public ASTNode<?> getParent() {
      return parent;
    }

    @Override
    public void setParent(Node<?> parent) {
      this.parent = (ASTNode<?>) parent;
    }

  }
}
