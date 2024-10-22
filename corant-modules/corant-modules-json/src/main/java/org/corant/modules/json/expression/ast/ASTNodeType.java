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

import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.strip;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTBetweenNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTEqualNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTGreaterThanEqualNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTGreaterThanNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTInNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTLessThanEqualNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTLessThanNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTNoEqualNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTNoInNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTRegexNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTEqualStrictlyNode;
import org.corant.modules.json.expression.ast.ASTComparisonNode.ASTNoEqualStrictlyNode;
import org.corant.modules.json.expression.ast.ASTFunctionNode.ASTDefaultFunctionNode;
import org.corant.modules.json.expression.ast.ASTLogicNode.ASTLogicAndNode;
import org.corant.modules.json.expression.ast.ASTLogicNode.ASTLogicNorNode;
import org.corant.modules.json.expression.ast.ASTLogicNode.ASTLogicNotNode;
import org.corant.modules.json.expression.ast.ASTLogicNode.ASTLogicOrNode;
import org.corant.modules.json.expression.ast.ASTLogicNode.ASTLogicXorNode;
import org.corant.modules.json.expression.ast.ASTVariableNode.ASTDefaultVariableNode;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:13:27
 */
public enum ASTNodeType {

  /**
   * The subroutine node.
   */
  SUBROUTINE("$sub", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTSubroutineNode();
    }
  },

  /**
   * Performs a conditional expressions.
   */
  CONDITIONAL("$if", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTConditionalNode();
    }
  },

  /**
   * Performs a loop expressions. FIXME uncompleted
   */
  EACH("$each", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTEachNode();
    }
  },

  /**
   * The return node.
   */
  RETURN("$ret", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTReturnNode();
    }
  },

  /**
   * Performs an AND operation on an array with at least two expressions and returns the objects
   * that meets all the expressions.
   */
  LG_AND("$and", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicAndNode();
    }
  },

  /**
   * Performs a NOT operation on the specified expression and returns the objects that do not meet
   * the expression.
   */
  LG_NOT("$not", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicNotNode();
    }
  },

  /**
   * Performs an OR operation on an array with at least two expressions and returns the objects that
   * meet at least one of the expressions.
   */
  LG_OR("$or", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicOrNode();
    }
  },

  /**
   * Performs a NOR operation on an array with at least two expressions and returns the objects that
   * do not meet any of the expressions.
   */
  LG_NOR("$nor", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicNorNode();
    }
  },

  /**
   * Performs an XOR operation on an array with at least two expressions and returns the objects
   * that do not meet any of the expressions.
   */
  LG_XOR("$xor", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicXorNode();
    }
  },

  // Performs a lambda operation on an array. examples: {"λ(x,y)":...]
  // LAMBDA("λ", false) {
  // @Override
  // public ASTNode<?> buildNode(Object object) {
  // return new ASTTernaryNode();
  // }
  // },

  /**
   * The equality comparator operator.
   */
  CP_EQ("$eq", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTEqualNode();
    }
  },

  /**
   * The strict equality comparator operator.
   */
  CP_EQS("$eqs", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTEqualStrictlyNode();
    }
  },

  /**
   * The inequality comparator operator.
   */
  CP_NE("$ne", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNoEqualNode();
    }
  },

  /**
   * The strict inequality comparator operator.
   */
  CP_NES("$nes", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNoEqualStrictlyNode();
    }
  },

  /**
   * The IN comparison operator. Use only one data type in the specified values, Use for element and
   * collection comparison.
   */
  CP_IN("$in", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTInNode();
    }
  },

  /**
   * The NOT-IN comparison operator, Use for element and collection comparison.
   */
  CP_NIN("$nin", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNoInNode();
    }
  },

  /**
   * The less than comparison operator.
   */
  CP_LT("$lt", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLessThanNode();
    }
  },

  /**
   * The less than or equals comparison operator.
   */
  CP_LTE("$lte", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLessThanEqualNode();
    }
  },

  /**
   * The greater than comparison operator.
   */
  CP_GT("$gt", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTGreaterThanNode();
    }
  },

  /**
   * The greater than or equals comparison operator.
   */
  CP_GTE("$gte", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTGreaterThanEqualNode();
    }
  },

  /**
   * The BETWEEN match operator.
   */
  CP_BTW("$btw", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTBetweenNode();
    }
  },

  /**
   * The regular expression predicate.
   */
  CP_REGEX("$regex", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTRegexNode();
    }
  },

  /**
   * The return node.
   */
  MAP("$map", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTMapNode();
    }
  },

  /**
   * The return node.
   */
  REDUCE("$reduce", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTReduceNode();
    }
  },

  /**
   * The return node.
   */
  FILTER("$filter", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTFilterNode();
    }
  },

  /**
   * Distinct
   */
  DISTINCT("$distinct", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDistinctNode();
    }
  },

  /**
   * The collect node.
   */
  COLLECT("$collect", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTCollectNode();
    }
  },

  /**
   * The sort node.
   */
  SORT("$sort", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTSortNode();
    }
  },

  /**
   * The max node.
   */
  MAX("$max", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTMaxNode();
    }
  },

  /**
   * The min node.
   */
  MIN("$min", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTMinNode();
    }
  },

  /**
   * The null value logic node.
   */
  NVL("$nvl", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNVLNode();
    }
  },

  /**
   * The non null value predicate node.
   */
  NON_NULL("$nonNull", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNullCheckNode(true);
    }
  },

  /**
   * The non null value predicate node.
   */
  IS_NULL("$isNull", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNullCheckNode(false);
    }
  },

  /**
   * The function node
   */
  FUN("#", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDefaultFunctionNode(object.toString().substring(1));
    }
  },

  /**
   * The variable node
   */
  VAR("@", true) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDefaultVariableNode(object.toString().substring(1));
    }
  },

  /**
   * The declaration node
   */
  DEC("", true) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDeclarationNode(object);
    }
  },
  /**
   * The value node
   */
  VALUE("", true) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTValueNode(object);
    }
  },

  /**
   * The array node
   */
  ARRAY("", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTArrayNode();
    }
  },

  /**
   * The object node
   */
  OBJECT("", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTObjectNode();
    }
  };

  final String token;
  final boolean leaf;

  ASTNodeType(String token, boolean leaf) {
    this.token = token;
    this.leaf = leaf;
  }

  public static ASTNodeType decideType(Object token) {
    if (token instanceof String) {
      String useToken;
      if (!isBlank(useToken = strip((String) token))) {
        if (useToken.startsWith(ASTNodeType.VAR.token)
            && useToken.length() > ASTNodeType.VAR.token.length()) {
          return ASTNodeType.VAR;
        } else if (useToken.startsWith(ASTNodeType.FUN.token)
            && useToken.length() > ASTNodeType.FUN.token.length()) {
          return ASTNodeType.FUN;
        } else if (useToken.startsWith("(") && useToken.endsWith(")")) {
          return ASTNodeType.DEC;
        } else {
          for (ASTNodeType t : ASTNodeType.values()) {
            if (t != ASTNodeType.FUN && t != ASTNodeType.VAR
                && t.token().equalsIgnoreCase(useToken)) {
              return t;
            }
          }
        }
      }
    }
    return null;
  }

  public abstract ASTNode<?> buildNode(Object object);

  public boolean isLeaf() {
    return leaf;
  }

  public String token() {
    return token;
  }

}
