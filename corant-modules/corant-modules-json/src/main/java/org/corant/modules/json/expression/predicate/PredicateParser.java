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
package org.corant.modules.json.expression.predicate;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Primitives.isPrimitiveOrWrapper;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.corant.modules.json.Jsons;
import org.corant.modules.json.expression.predicate.ast.ASTNode;
import org.corant.modules.json.expression.predicate.ast.ASTNodeType;
import org.corant.modules.json.expression.predicate.ast.ASTPredicateNode;
import org.corant.modules.json.expression.predicate.ast.ASTVisitor;
import org.corant.modules.json.expression.predicate.ast.ASTVisitor.ASTDefaultVisitor;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:44:37
 *
 */
public class PredicateParser {

  public static Node<Boolean> parse(String json) {
    Map<String, Object> map = Jsons.fromString(json);
    shouldBeTrue(isNotEmpty(map) && sizeOf(map) == 1,
        () -> new ParseException("The syntax error!"));
    Entry<String, Object> entry = map.entrySet().iterator().next();
    String key = entry.getKey();
    Object val = entry.getValue();
    ASTPredicateNode root =
        shouldInstanceOf(shouldNotNull(ASTNodeType.decideNode(key)), ASTPredicateNode.class);
    parse(new ASTDefaultVisitor(), root, val);
    return root;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void parse(ASTVisitor visitor, Node<?> parent, Object val) {
    if (val == null || isSimpleClass(val.getClass())) {
      parseSingle(visitor, parent, val);
    } else if (val instanceof Collection) {
      for (Object ele : (Collection<?>) val) {
        parse(visitor, parent, ele);
      }
    } else if (val instanceof Object[]) {
      for (Object ele : (Object[]) val) {
        parse(visitor, parent, ele);
      }
    } else if (val instanceof Map) {
      ((Map) val).forEach((k, v) -> {
        if (isSimpleClass(shouldNotNull(k).getClass())) {
          ASTNode<?> keyNode = ASTNodeType.decideNode(k);
          if (keyNode.getType().isLeaf()) {
            parseSingle(visitor, parent, k);
            parse(visitor, parent, v);
          } else {
            parent.addChild(keyNode);
            parse(visitor, keyNode, v);
          }
        } else {
          throw new NotSupportedException();
        }
      });
    } else {
      throw new NotSupportedException();
    }
  }

  static void parseSingle(ASTVisitor visitor, Node<?> parent, Object object) {
    ASTNode<?> vn = ASTNodeType.decideNode(object);
    if (visitor.supports(vn.getType())) {
      visitor.prepare(vn);
      parent.addChild(vn);
      visitor.visit(vn);
    } else {
      parent.addChild(vn);
    }
  }

  private static boolean isSimpleClass(Class<?> type) {
    return isPrimitiveOrWrapper(type) || String.class.equals(type)
        || Number.class.isAssignableFrom(type) || Date.class.isAssignableFrom(type)
        || Enum.class.isAssignableFrom(type) || Timestamp.class.isAssignableFrom(type)
        || Temporal.class.isAssignableFrom(type) || URL.class.isAssignableFrom(type)
        || URI.class.isAssignableFrom(type) || TemporalAmount.class.isAssignableFrom(type)
        || Currency.class.isAssignableFrom(type) || Locale.class.isAssignableFrom(type)
        || TimeZone.class.isAssignableFrom(type);
  }
}
