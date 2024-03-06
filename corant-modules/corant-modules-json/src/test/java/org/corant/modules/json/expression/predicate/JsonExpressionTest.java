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

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.linkedHashMapOf;
import static org.corant.shared.util.Maps.mapOf;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.corant.modules.json.Jsons;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.EvaluationContext.DefaultEvaluationContext;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.modules.json.expression.Node;
import org.corant.modules.json.expression.SimpleParser;
import org.corant.modules.json.expression.ast.ASTFunctionNode;
import org.corant.modules.json.expression.ast.ASTVariableNode;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Maps;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-json
 *
 * @author bingo 下午4:24:48
 */
public class JsonExpressionTest extends TestCase {

  public static void main(String[] args) {
    Map<String, Object> map =
        mapOf("a", 1, "b", listOf(mapOf("b1", mapOf("b2", "b2")), mapOf("b1", mapOf("b2", "b2"))));
    String[] path = {"b"};
    Object val = Maps.getMapKeyPathValues(map, path, false);
    System.out.println(Jsons.toString(val, true));
    System.out.println("=".repeat(80));
    val = Maps.getMapKeyPathValues(map, path, true);
    System.out.println(Jsons.toString(val, true));
  }

  @Test
  public void testCollect() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    System.out.println(list.stream().map(x -> getMapString(x, "name")).collect(ArrayList::new,
        ArrayList::add, ArrayList::addAll));

    Object mapper = linkedHashMapOf("$map",
        mapOf("@list", mapOf("(e)", mapOf("#java.util.Map::get", new Object[] {"@e", "name"}))));
    Map<String, Object> exp = linkedHashMapOf("$collect",
        new Object[] {mapper, mapOf("#java.util.ArrayList::new", new Object[0]),
            mapOf("(e,r)", mapOf("#java.util.ArrayList::add", new Object[] {"@e", "@r"})),
            mapOf("(e,r)",
                mapOf("$sub",
                    mapOf("(s)",
                        listOf(mapOf("#java.util.ArrayList::remvoeAll", new Object[] {"@l", "@r"}),
                            mapOf("$ret", "@l")))))});

    System.out.println(Jsons.toString(exp, true));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));

  }

  @Test
  public void testFilter() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "edf", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "hij", "now", Instant.now()));
    Map<String, Object> exp = linkedHashMapOf("$filter",
        mapOf("@list", mapOf("(e)", mapOf("$gt", new Object[] {"@e.id", 1}))));
    System.out.println(Jsons.toString(exp, true));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
  }

  @Test
  public void testFunc() {
    final Map<String, Object> r = mapOf("r.id", 123, "r.name", "bingo.chen", "r.a", 100, "r.b",
        "10  ", "r.s", 0, "r.e", 7, "r.indexof", "c", "r.size");
    final EvaluationContext ec = new EvaluationContext() {
      @Override
      public Function<Object[], Object> resolveFunction(Node<?> node) {
        ASTFunctionNode fn = (ASTFunctionNode) node;
        Optional<FunctionResolver> fr =
            SimpleParser.resolveFunction().filter(p -> p.supports(fn.getName())).findFirst();
        if (fr.isPresent()) {
          return fr.get().resolve(fn.getName());
        }
        throw new CorantRuntimeException("xxx");
      }

      @Override
      public Object resolveVariableValue(Node<?> node) {
        return r.get(((ASTVariableNode) node).getName());
      }
    };

    String[] exps = {"{\"#java.lang.String::substring\":[\"@r.name\",\"@r.s\",\"@r.e\"]}",
        "{\"#java.lang.String::indexOf\":[\"@r.name\",\"@r.indexof\"]}",
        "{\"#java.lang.System::currentTimeMillis\":[]}", "{\"#java.util.Date::new\":[]}",
        "{\"#java.util.UUID::toString\":[{\"#java.util.UUID::randomUUID\":[]}]}",
        "{\"#sizeOf\":\"@r.name\"}", "{\"#sizeOf\":\"@r.size\"}",
        "{\"$if\":[{\"$gt\":[{\"#sizeOf\":\"@r.name\"},9]},\"yes\",\"no\"]}"};
    Node<?>[] nodes = Arrays.stream(exps).map(SimpleParser::parse).toArray(Node[]::new);
    for (Node<?> node : nodes) {
      Object val = node.getValue(ec);
      val.toString();
      System.out.println("[" + val + "]");
    }
  }

  @Test
  public void testMap() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "edf", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "hij", "now", Instant.now()));
    Object mapper = mapOf("#java.util.Map::get", new Object[] {"@e", "id"});
    mapper = mapOf("#org.corant.shared.util.Maps::linkedHashMapOf",
        new Object[] {"id", "@e.id", "name", "@e.name"});
    mapper = mapOf("$sub", listOf(
        mapOf("#java.util.Map::put", new Object[] {"@e", "xxx",
            mapOf("$if", listOf(mapOf("$gt", listOf("@e.id", 1)), mapOf("$sub",
                mapOf("(k)", listOf(mapOf("#java.lang.String::concat", listOf("@e.name", "------")),
                    mapOf("#org.corant.shared.util.Strings::substring", listOf("@k", -3)),
                    mapOf("#java.lang.String::concat", listOf("@k", "+++")), mapOf("$ret", "@k")))),
                "9999"))}),
        mapOf("#java.util.Map::put",
            new Object[] {"@e", "put",
                mapOf("#java.util.Map::containsKey", new Object[] {"@e", "xxx"})}),
        mapOf("$ret", "@e")));

    Map<String, Object> exp = linkedHashMapOf("$map", mapOf("@list", mapOf("(e)", mapper)));
    System.out.println(Jsons.toString(exp, true));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
  }

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

  @Test
  public void testReduce() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    System.out.println(list.stream().map(x -> getMapString(x, "name")).reduce(new StringBuffer(),
        StringBuffer::append, StringBuffer::append));

    Object mapper = linkedHashMapOf("$map",
        mapOf("@list", mapOf("(e)", mapOf("#java.util.Map::get", new Object[] {"@e", "name"}))));
    Map<String, Object> exp = linkedHashMapOf("$reduce",
        new Object[] {mapper, mapOf("#java.lang.StringBuffer::new", new Object[0]),
            mapOf("(e,r)", mapOf("#java.lang.StringBuffer::append", new Object[] {"@e", "@r"})),
            mapOf("(e,r)", mapOf("#java.lang.StringBuffer::append", new Object[] {"@e", "@r"}))});

    System.out.println(Jsons.toString(exp, true));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));

  }
}
