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

import static org.corant.shared.util.Strings.split;
import java.util.Arrays;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.shared.util.Strings;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:04:44
 */
public class ASTDeclarationNode implements ASTNode<Object> {
  protected ASTNode<?> parent;
  protected final Object value;
  protected String[] variableNames;

  public ASTDeclarationNode(Object value) {
    this.value = value;
    variableNames = parseVariableNames(value.toString());
  }

  public static String[] parseVariableNames(String varNames) {
    if (varNames.startsWith("(") && varNames.endsWith(")")) {
      return split(varNames.substring(1, varNames.length() - 1), ",", true, true);
    }
    return Strings.EMPTY_ARRAY;
  }

  public static String[] variableNamesOf(Node<?> node, EvaluationContext ctx) {
    return parseVariableNames(node.getValue(ctx).toString());
  }

  @Override
  public ASTNode<?> getParent() {
    return parent;
  }

  @Override
  public ASTNodeType getType() {
    return ASTNodeType.DEC;
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    return value;
  }

  public String[] getVariableNames() {
    return Arrays.copyOf(variableNames, variableNames.length);
  }

  @Override
  public void setParent(Node<?> parent) {
    this.parent = (ASTNode<?>) parent;
  }

  public Object value() {
    return value;
  }
}
