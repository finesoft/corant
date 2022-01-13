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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;

/**
 * corant-modules-json
 *
 * @author bingo 下午9:27:26
 *
 */
public interface ASTLogicNode extends ASTPredicateNode {

  @Override
  List<ASTPredicateNode> getChildren();

  abstract class AbstractASTLogicNode implements ASTLogicNode {

    protected final List<ASTPredicateNode> children = new ArrayList<>();
    protected final ASTNodeType type;

    AbstractASTLogicNode(ASTNodeType type) {
      this.type = type;
    }

    @Override
    public boolean addChild(Node<?> child) {
      return children.add(shouldInstanceOf(child, ASTPredicateNode.class));
    }

    @Override
    public List<ASTPredicateNode> getChildren() {
      return children;
    }

    @Override
    public ASTNodeType getType() {
      return type;
    }

    protected boolean removeChildIf(Predicate<ASTPredicateNode> filter) {
      return children.removeIf(filter);
    }
  }

  class ASTLogicAndNode extends AbstractASTLogicNode {

    public ASTLogicAndNode() {
      super(ASTNodeType.LG_AND);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return children.stream().allMatch(n -> n.getValue(ctx));
    }
  }

  class ASTLogicNorNode extends AbstractASTLogicNode {

    public ASTLogicNorNode() {
      super(ASTNodeType.LG_NOR);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return children.stream().noneMatch(n -> n.getValue(ctx));
    }
  }

  class ASTLogicNotNode extends AbstractASTLogicNode {

    public ASTLogicNotNode() {
      super(ASTNodeType.LG_NOT);
    }

    @Override
    public boolean addChild(Node<?> child) {
      children.clear();
      return super.addChild(child);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return !children.get(0).getValue(ctx);
    }

  }

  class ASTLogicOrNode extends AbstractASTLogicNode {

    public ASTLogicOrNode() {
      super(ASTNodeType.LG_OR);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return children.stream().anyMatch(n -> n.getValue(ctx));
    }
  }

  class ASTLogicXorNode extends AbstractASTLogicNode {

    public ASTLogicXorNode() {
      super(ASTNodeType.LG_XOR);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return Boolean.logicalXor(children.get(0).getValue(ctx), children.get(1).getValue(ctx));
    }
  }

}
