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

import static org.corant.shared.util.Maps.mapOf;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.SimpleParser;
import org.corant.modules.json.expression.ast.ASTFunctionNode;
import org.corant.modules.json.expression.ast.ASTVariableNode;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-json
 *
 * @author bingo 下午4:24:48
 *
 */
public class PredicateTest extends TestCase {

  @Test
  public void testMixed() {
    Map<String, Object> r = mapOf("r.id", 123, "r.name", "bingo.chen", "r.a", 100, "r.b", "10");
    String exp =
        "{\"$and\":[{\"$eq\":[{\"#add\":[\"@r.a\", {\"#convert\":[\"@r.b\",\"java.lang.Integer\"]},13]},123]},{\"$eq\":{\"@r.name\":{\"#xxx\":\"bingo\"}}}]}";
    Node<?> node = SimpleParser.parse(exp);
    assertTrue((Boolean) node.getValue(new EvaluationContext() {
      @Override
      public Function<Object[], Object> resolveFunction(Node<?> node) {
        ASTFunctionNode fn = (ASTFunctionNode) node;
        Optional<FunctionResolver> fr =
            SimpleParser.resolveFunction().filter(p -> p.supports(fn.getName())).findFirst();
        if (fr.isPresent()) {
          return fr.get().resolve(fn.getName());
        } else {
          return p -> p[0] + ".chen";
        }
      }

      @Override
      public Object resolveVariableValue(Node<?> node) {
        return r.get(((ASTVariableNode) node).getName());
      }
    }));
  }
}
