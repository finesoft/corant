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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Primitives.isSimpleClass;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.corant.modules.json.Jsons;
import org.corant.modules.json.expression.ast.ASTNode;
import org.corant.modules.json.expression.ast.ASTNodeBuilder;
import org.corant.modules.json.expression.ast.ASTNodeType;
import org.corant.modules.json.expression.ast.ASTNodeVisitor;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Services;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:44:37
 *
 */
public class SimpleParser {

  public static Node<?> parse(Map<String, Object> map, ASTNodeBuilder builder) {
    return parse(map, builder, resolveVisitor());
  }

  public static Node<?> parse(Map<String, Object> map, ASTNodeBuilder builder,
      ASTNodeVisitor visitor) {
    shouldBeTrue(isNotEmpty(map) && sizeOf(map) == 1,
        () -> new ParseException("Syntax error, only one root node is accepted!"));
    final Entry<String, Object> entry = map.entrySet().iterator().next();
    final String key = entry.getKey();
    final Object val = entry.getValue();
    final ASTNode<?> root = shouldNotNull(builder.build(key));
    if (visitor.supports(root.getType())) {
      visitor.prepare(root);
    }
    parse(builder, visitor, root, val);
    if (visitor.supports(root.getType())) {
      visitor.visit(root);
    }
    return root;
  }

  public static Node<?> parse(String json) {
    return parse(json, resolveBuilder(), resolveVisitor());
  }

  public static Node<?> parse(String json, ASTNodeBuilder builder) {
    return parse(json, builder, resolveVisitor());
  }

  public static Node<?> parse(String json, ASTNodeBuilder builder, ASTNodeVisitor visitor) {
    return parse(Jsons.fromString(json), builder, visitor);
  }

  public static ASTNodeBuilder resolveBuilder() {
    return Services.find(ASTNodeBuilder.class).orElse(ASTNodeBuilder.DFLT);
  }

  public static Stream<FunctionResolver> resolveFunction() {
    return Services.select(FunctionResolver.class);
  }

  public static ASTNodeVisitor resolveVisitor() {
    return Services.find(ASTNodeVisitor.class).orElse(ASTNodeVisitor.DFLT);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void parse(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent, Object val) {
    if (val == null || isSimpleClass(val.getClass())) {
      parseSingle(builder, visitor, parent, val);
    } else if (val instanceof Collection) {
      for (Object ele : (Collection<?>) val) {
        parse(builder, visitor, parent, ele);
      }
    } else if (val instanceof Object[]) {
      for (Object ele : (Object[]) val) {
        parse(builder, visitor, parent, ele);
      }
    } else if (val instanceof Map) {
      ((Map) val).forEach((k, v) -> {
        if (isSimpleClass(shouldNotNull(k).getClass())) {
          if (ASTNodeType.decideType(k).isLeaf()) {
            parseSingle(builder, visitor, parent, k);
            parse(builder, visitor, parent, v);
          } else {
            ASTNode<?> subNode = builder.build(k);
            if (visitor.supports(subNode.getType())) {
              visitor.prepare(subNode);
            }
            parent.addChild(subNode);
            parse(builder, visitor, subNode, v);
            if (visitor.supports(subNode.getType())) {
              visitor.visit(subNode);
            }
          }
        } else {
          throw new NotSupportedException();
        }
      });
    } else {
      throw new NotSupportedException();
    }
  }

  static void parseSingle(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent,
      Object token) {
    ASTNode<?> node = builder.build(token);
    if (visitor.supports(node.getType())) {
      visitor.prepare(node);
      parent.addChild(node);
      visitor.visit(node);
    } else {
      parent.addChild(node);
    }
  }

}
