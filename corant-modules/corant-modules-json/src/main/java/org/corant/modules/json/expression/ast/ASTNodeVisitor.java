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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTRegexNode;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:01:15
 *
 */
public interface ASTNodeVisitor extends Sortable {

  ASTNodeVisitor DFLT = node -> {
    switch (node.getType()) {
      case CP_EQ:
      case CP_GT:
      case CP_GTE:
      case CP_LT:
      case CP_LTE:
      case CP_NE:
      case LG_XOR:
        shouldBeTrue(node.getChildren().size() == 2);
        break;
      case CP_REGEX:
        shouldBeTrue(node.getChildren().size() == 2
            && ((ASTNode<?>) node.getChildren().get(1)).getType() == ASTNodeType.VAL);
        ((ASTRegexNode) node).initialize();
        break;
      case CP_BTW:
        shouldBeTrue(node.getChildren().size() == 3);
        break;
      case CP_IN:
      case CP_NIN:
      case LG_AND:
      case LG_NOT:
      case LG_NOR:
      case LG_OR:
        shouldBeTrue(node.getChildren().size() > 0);
        break;
      default:
        break;
    }
  };

  default void prepare(ASTNode<?> node) {}

  default boolean supports(ASTNodeType type) {
    return true;
  }

  void visit(ASTNode<?> node);

}
