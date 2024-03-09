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
package org.corant.modules.json.expression;

import java.util.List;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:23:27
 */
public interface Node<T> {

  boolean addChild(Node<?> child);

  List<? extends Node<?>> getChildren();

  Node<?> getParent();

  T getValue(EvaluationContext ctx);

  void setParent(Node<?> parent);
}
