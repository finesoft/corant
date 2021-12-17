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

import java.util.List;
import org.corant.modules.json.expression.Node;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:04:44
 *
 */
public interface ASTNode<T> extends Node<T> {

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

}
