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

import java.util.Arrays;
import java.util.stream.Collectors;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;
import org.corant.shared.util.Streams;

/**
 * corant-modules-json
 *
 * @author bingo 17:37:19
 */
public class ASTDistinctNode extends AbstractASTNode<Object> {

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.DISTINCT;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {

    Node<?> inputNode = children.get(0);
    final Object input = inputNode.getValue(ctx);

    if (input instanceof Object[] array) {
      return Arrays.stream(array).distinct().toArray();
    } else if (input instanceof Iterable<?> itr) {
      return Streams.streamOf(itr).distinct().collect(Collectors.toList());
    } else {
      return input;
    }
  }

}
