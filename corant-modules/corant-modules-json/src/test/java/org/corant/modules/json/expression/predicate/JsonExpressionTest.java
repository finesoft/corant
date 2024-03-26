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
    System.out.println(list.stream().map(x -> getMapString(x, "name"))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll).toString());

    Object mapper = linkedHashMapOf("$map",
        mapOf("@list", mapOf("(e)", mapOf("#Map:get", new Object[] {"@e", "name"}))));
    Map<String, Object> exp = linkedHashMapOf("$collect", new Object[] {mapper, "#ArrayList::new",
        mapOf("(e,r)", mapOf("#ArrayList:add", new Object[] {"@e", "@r"}))});

    System.out.println(Jsons.toString(exp, true));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));

  }

  @Test
  public void testCompare() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 12, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 23, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 34, "name", "ghi", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    Map<String, Object> max = mapOf("$max",
        listOf("@list", "(t1,t2)",
            mapOf("#Integer::compare", listOf(mapOf("#Maps::getMapInteger", listOf("@t1", "id")),
                mapOf("#Maps::getMapInteger", listOf("@t2", "id"))))));
    Map<String, Object> min = mapOf("$min",
        listOf("@list", "(t1,t2)",
            mapOf("#Integer::compare", listOf(mapOf("#Maps::getMapInteger", listOf("@t1", "id")),
                mapOf("#Maps::getMapInteger", listOf("@t2", "id"))))));
    Node<?> maxEval = SimpleParser.parse(max, SimpleParser.resolveBuilder());
    System.out.println(Jsons.toString(max, true));
    System.out.println("-".repeat(100));
    Object result = maxEval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
    System.out.println("=".repeat(100));
    Node<?> minEval = SimpleParser.parse(min, SimpleParser.resolveBuilder());
    System.out.println(Jsons.toString(min, true));
    System.out.println("-".repeat(100));
    result = minEval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
  }

  @Test
  public void testDistinct() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    Map<String, Object> exp = mapOf("$distinct", "@list");
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
    result = eval.getValue(new DefaultEvaluationContext("list", list.get(0)));
    System.out.println(Jsons.toString(result, true));
    result = eval.getValue(new DefaultEvaluationContext("list", null));
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
    Object result = null;
    DefaultEvaluationContext ctx = new DefaultEvaluationContext("list", list);
    for (int i = 0; i < 10000; i++) {
      result = eval.getValue(ctx);
    }
    long t1 = System.currentTimeMillis();
    ctx = new DefaultEvaluationContext("list", list);
    for (int i = 0; i < 100000; i++) {
      result = eval.getValue(ctx);
    }
    System.out.println(Jsons.toString(result, true));
    System.out.println(System.currentTimeMillis() - t1);
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

    String[] exps = {"{\"#String:substring\":[\"@r.name\",\"@r.s\",\"@r.e\"]}",
        "{\"#String:indexOf\":[\"@r.name\",\"@r.indexof\"]}", "{\"#System::currentTimeMillis\":[]}",
        "{\"#Date::new\":[]}", "{\"#UUID:toString\":[{\"#UUID::randomUUID\":[]}]}",
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
    Object mapper = mapOf("#Map:get", new Object[] {"@e", "id"});
    mapper = mapOf("#Maps::linkedHashMapOf", new Object[] {"id", "@e.id", "name", "@e.name"});
    mapper = mapOf("$sub", listOf(
        mapOf("#Map:put",
            new Object[] {"@e", "xxx",
                mapOf("$if", listOf(mapOf("$gt", listOf("@e.id", 1)), mapOf("$sub",
                    mapOf("(k)", listOf(mapOf("#String:concat", listOf("@e.name", "------")),
                        // mapOf("#Strings::substring", listOf("@k", -3)),
                        mapOf("#String:substring",
                            listOf("@k", 0,
                                mapOf("#sub", listOf(mapOf("#String:length", listOf("@k")), 3)))),

                        mapOf("#String:concat", listOf("@k", "+++")), mapOf("$ret", "@k")))),
                    "9999"))}),
        mapOf("#Map:put",
            new Object[] {"@e", "put", mapOf("#Map:containsKey", new Object[] {"@e", "xxx"})}),
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
        mapOf("@list", mapOf("(e)", mapOf("#Map:get", new Object[] {"@e", "name"}))));
    Map<String, Object> exp = linkedHashMapOf("$reduce",
        new Object[] {mapper, mapOf("#StringBuilder::new", new Object[0]),
            mapOf("(e,r)", mapOf("#StringBuilder:append", new Object[] {"@e", "@r"}))});

    System.out.println(Jsons.toString(exp, true));
    System.out.println("=".repeat(100));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    Object result = null;
    EvaluationContext ctx = new DefaultEvaluationContext("list", list);
    for (int i = 0; i < 1000; i++) {
      result = eval.getValue(ctx);
    }
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      result = eval.getValue(ctx);
    }
    System.out.println(Jsons.toString(result, true));
    System.out.println(System.currentTimeMillis() - t1);
    System.out.println("=".repeat(100));
    for (int i = 0; i < 1000; i++) {
      result = list.stream().map(e -> e.get("name").toString())
          .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }
    t1 = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      result = list.stream().map(e -> e.get("name").toString())
          .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }
    System.out.println(Jsons.toString(result, true));
    System.out.println(System.currentTimeMillis() - t1);
  }

  @Test
  public void testSort() {
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(linkedHashMapOf("id", 12, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 23, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 34, "name", "ghi", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 1, "name", "abc", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 2, "name", "def", "now", Instant.now()));
    list.add(linkedHashMapOf("id", 3, "name", "ghi", "now", Instant.now()));
    Map<String, Object> exp = mapOf("$sort",
        listOf("@list", "(t1,t2)",
            mapOf("#Integer::compare", listOf(mapOf("#Maps::getMapInteger", listOf("@t1", "id")),
                mapOf("#Maps::getMapInteger", listOf("@t2", "id"))))));
    Node<?> eval = SimpleParser.parse(exp, SimpleParser.resolveBuilder());
    System.out.println(Jsons.toString(exp, true));
    System.out.println("=".repeat(100));
    Object result = eval.getValue(new DefaultEvaluationContext("list", list));
    System.out.println(Jsons.toString(result, true));
    result = eval.getValue(new DefaultEvaluationContext("list", list.get(0)));
    System.out.println(Jsons.toString(result, true));
    result = eval.getValue(new DefaultEvaluationContext("list", null));
    System.out.println(Jsons.toString(result, true));
  }
}
