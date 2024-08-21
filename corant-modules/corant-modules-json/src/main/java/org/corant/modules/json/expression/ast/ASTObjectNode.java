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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.ParseException;
import org.corant.modules.json.expression.ast.ASTNode.AbstractASTNode;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-json
 *
 * @author bingo 18:19:29
 */
public class ASTObjectNode extends AbstractASTNode<Object> {

  protected boolean keyValue;

  public ASTObjectNode() {}

  @Override
  public boolean addChild(Node<?> child) {
    ASTNode<?> childNode = (ASTNode<?>) child;
    if (childNode instanceof EntryNode) {
      keyValue = true;
      if (!children.isEmpty()) {
        shouldBeTrue(children.stream().allMatch(EntryNode.class::isInstance),
            () -> new ParseException(
                "All children of an object node are either all values or all key-value pairs"));
      }
    }
    return children.add(childNode);
  }

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.OBJECT;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getValue(EvaluationContext ctx) {
    if (children.isEmpty()) {
      return new HashMap<>();
    }
    if (keyValue) {
      Map<String, Object> map = new LinkedHashMap<>();
      children.stream().map(c -> c.getValue(ctx)).forEach(v -> {
        Pair<String, Object> pair = (Pair<String, Object>) v;
        map.put(pair.getKey(), pair.getValue());
      });
      return map;
    } else {
      // FIXME is array?
      if (children.size() == 1) {
        return children.get(0).getValue(ctx);
      }
      return children.stream().map(c -> c.getValue(ctx)).collect(Collectors.toList());
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 18:30:26
   */
  public static class EntryNode implements ASTNode<Object> {
    protected ASTNode<?> parent;
    protected final String key;
    protected ASTNode<?> valueNode;

    public EntryNode(String key, ASTNode<?> valueNode) {
      this.key = key;
      this.valueNode = valueNode;
    }

    @Override
    public ASTNode<?> getParent() {
      return parent;
    }

    @Override
    public ASTNodeType getType() {
      return ASTNodeType.OBJECT;
    }

    @Override
    public Pair<String, Object> getValue(EvaluationContext ctx) {
      return Pair.of(key, valueNode.getValue(ctx));
    }

    @Override
    public void setParent(Node<?> parent) {
      this.parent = (ASTNode<?>) parent;
    }
  }
}
