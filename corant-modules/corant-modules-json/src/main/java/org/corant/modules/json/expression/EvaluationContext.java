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

import static org.corant.shared.normal.Names.splitNameSpace;
import static org.corant.shared.util.Maps.getMapKeyPathValue;
import static org.corant.shared.util.Maps.mapOf;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.corant.modules.json.ObjectMappers;
import org.corant.modules.json.expression.ast.ASTFunctionNode;
import org.corant.modules.json.expression.ast.ASTVariableNode;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:23:09
 */
public interface EvaluationContext extends Sortable {

  Function<Object[], Object> resolveFunction(Node<?> node);

  Object resolveVariableValue(Node<?> node);

  /**
   * corant-modules-json
   *
   * @author bingo 16:40:44
   */
  class BindableEvaluationContext implements EvaluationContext {

    protected final EvaluationContext original;
    protected final Map<String, Object> bindings = new LinkedHashMap<>();

    public BindableEvaluationContext(EvaluationContext original) {
      this.original = original;
    }

    public BindableEvaluationContext bind(String name, Object value) {
      bindings.put(name, value);
      return this;
    }

    @Override
    public Function<Object[], Object> resolveFunction(Node<?> node) {
      return original.resolveFunction(node);
    }

    @Override
    public Object resolveVariableValue(Node<?> node) {
      if (node instanceof ASTVariableNode varNode) {
        String[] varNamePath = splitNameSpace(varNode.getName(), true, false);
        if (bindings.containsKey(varNamePath[0])) {
          Object boundValue = bindings.get(varNamePath[0]);
          if (varNamePath.length == 1) {
            return boundValue;
          } else if (boundValue instanceof Map boundMap) {
            return getMapKeyPathValue(boundMap,
                Arrays.copyOfRange(varNamePath, 1, varNamePath.length), false);
          } else if (boundValue != null) {
            return getMapKeyPathValue(ObjectMappers.toMap(boundValue),
                Arrays.copyOfRange(varNamePath, 1, varNamePath.length), false);
          } else {
            return null;
          }
        }
      }
      return original.resolveVariableValue(node);
    }

    public BindableEvaluationContext unbind(String name) {
      bindings.remove(name);
      return this;
    }

    public BindableEvaluationContext unbindAll() {
      bindings.clear();
      return this;
    }
  }

  /**
   * corant-modules-json
   *
   * @author bingo 16:58:46
   */
  class DefaultEvaluationContext implements EvaluationContext {

    final Map<String, Object> variables;

    public DefaultEvaluationContext(Object... objects) {
      variables = mapOf(objects);
    }

    @Override
    public Function<Object[], Object> resolveFunction(Node<?> node) {
      ASTFunctionNode funcNode = (ASTFunctionNode) node;
      return SimpleParser.resolveFunction().filter(fr -> fr.supports(funcNode.getName()))
          .min(Sortable::compare).orElseThrow(NotSupportedException::new)
          .resolve(funcNode.getName());
    }

    @Override
    public Object resolveVariableValue(Node<?> node) {
      ASTVariableNode varNode = (ASTVariableNode) node;
      return variables.get(varNode.getName());
    }

  }
}
