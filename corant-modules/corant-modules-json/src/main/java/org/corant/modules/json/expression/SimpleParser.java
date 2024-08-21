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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.json.ObjectMappers;
import org.corant.modules.json.expression.ast.ASTNode;
import org.corant.modules.json.expression.ast.ASTNodeBuilder;
import org.corant.modules.json.expression.ast.ASTNodeType;
import org.corant.modules.json.expression.ast.ASTNodeVisitor;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Services;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:44:37
 */
public class SimpleParser {

  static final ObjectMapper objectMapper = ObjectMappers.copyDefaultObjectMapper();

  public static Node<?> parse(Map<?, ?> map) {
    return parse(map, resolveBuilder(), resolveVisitor());
  }

  public static Node<?> parse(Map<?, ?> map, ASTNodeBuilder builder) {
    return parse(map, builder, resolveVisitor());
  }

  public static Node<?> parse(Map<?, ?> map, ASTNodeBuilder builder, ASTNodeVisitor visitor) {
    try {
      JsonNode jsonNode = objectMapper.valueToTree(map);
      return parse(builder, visitor, jsonNode);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static Node<?> parse(String json) {
    return parse(json, resolveBuilder(), resolveVisitor());
  }

  public static Node<?> parse(String json, ASTNodeBuilder builder) {
    return parse(json, builder, resolveVisitor());
  }

  public static Node<?> parse(String json, ASTNodeBuilder builder, ASTNodeVisitor visitor) {
    try {
      JsonNode jsonNode = objectMapper.readTree(json);
      return parse(builder, visitor, jsonNode);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static ASTNodeBuilder resolveBuilder() {
    return Services.findRequired(ASTNodeBuilder.class).orElse(ASTNodeBuilder.DFLT);
  }

  public static Stream<FunctionResolver> resolveFunction() {
    return Services.selectRequired(FunctionResolver.class);
  }

  public static ASTNodeVisitor resolveVisitor() {
    return Services.findRequired(ASTNodeVisitor.class).orElse(ASTNodeVisitor.DFLT);
  }

  static ASTNode<?> createASTNodeFromJsonValue(ASTNodeBuilder builder, JsonNode jsonNode)
      throws IOException {
    Object jsonNodeValue;
    if (jsonNode.isFloat()) {
      jsonNodeValue = jsonNode.floatValue();
    } else if (jsonNode.isDouble()) {
      jsonNodeValue = jsonNode.doubleValue();
    } else if (jsonNode.isBoolean()) {
      jsonNodeValue = jsonNode.booleanValue();
    } else if (jsonNode.isShort()) {
      jsonNodeValue = jsonNode.shortValue();
    } else if (jsonNode.isInt()) {
      jsonNodeValue = jsonNode.intValue();
    } else if (jsonNode.isLong()) {
      jsonNodeValue = jsonNode.longValue();
    } else if (jsonNode.isBigInteger()) {
      jsonNodeValue = jsonNode.bigIntegerValue();
    } else if (jsonNode.isBigDecimal()) {
      jsonNodeValue = jsonNode.decimalValue();
    } else if (jsonNode.isTextual()) {
      jsonNodeValue = jsonNode.textValue();
    } else if (jsonNode.isBinary()) {
      jsonNodeValue = jsonNode.binaryValue();
    } else if (jsonNode.isNull()) {
      jsonNodeValue = null;
    } else {
      throw new NotSupportedException("Can't support % json node!", jsonNode);
    }
    ASTNode<?> node = null;
    if (jsonNodeValue instanceof String name) {
      ASTNodeType astNodeType = ASTNodeType.decideType(name);
      if (astNodeType != null) {
        node = builder.build(name);
      }
    }
    if (node == null) {
      node = builder.valueNodeOf(jsonNodeValue);
    }
    return node;
  }

  static void makeRelation(ASTNodeVisitor visitor, Node<?> parent, ASTNode<?> node) {
    if (visitor.supports(node.getType())) {
      visitor.prepare(node);
      parent.addChild(node);
      visitor.visit(node);
    } else {
      parent.addChild(node);
    }
    node.postConstruct();
  }

  static Node<?> parse(ASTNodeBuilder builder, ASTNodeVisitor visitor, JsonNode jsonNode)
      throws Exception, IOException {
    ASTNode<?> root;
    if (jsonNode.isArray()) {
      root = builder.arrayNodeOf();
      parse(builder, visitor, root, jsonNode);
    } else if (jsonNode.isObject()) {
      root = builder.objectNode();
      parse(builder, visitor, root, jsonNode);
    } else {
      root = createASTNodeFromJsonValue(builder, jsonNode);
    }
    return root;
  }

  static void parse(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent,
      JsonNode jsonNode) throws Exception {
    if (jsonNode == null) {
      return;
    }
    if (jsonNode.isValueNode() || jsonNode.isNull()) {
      parseJsonValueNode(builder, visitor, parent, jsonNode);
    } else if (jsonNode.isArray()) {
      parseJsonArrayNode(builder, visitor, parent, jsonNode);
    } else if (jsonNode.isObject()) {
      parseJsonObjectNode(builder, visitor, parent, jsonNode);
    }
  }

  static void parseJsonArrayNode(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent,
      JsonNode jsonNode) throws IOException, Exception {
    Iterator<JsonNode> iterator = jsonNode.elements();
    while (iterator.hasNext()) {
      JsonNode subJsonNode = iterator.next();
      if (subJsonNode.isValueNode() || subJsonNode.isNull()) {
        parseJsonValueNode(builder, visitor, parent, subJsonNode);
      } else if (subJsonNode.isArray()) {
        ASTNode<?> subNode = builder.arrayNodeOf();
        parse(builder, visitor, subNode, subJsonNode);
        makeRelation(visitor, parent, subNode);
      } else if (subJsonNode.isObject()) {
        // check if element node contains non value node
        // FIXME should we allow object value node & computer node use together??
        boolean isObjectValueNode = true;
        Iterator<Map.Entry<String, JsonNode>> it = subJsonNode.fields();
        while (it.hasNext()) {
          Map.Entry<String, JsonNode> next = it.next();
          String name = next.getKey();
          JsonNode nextNode = next.getValue();
          ASTNodeType astNodeType = ASTNodeType.decideType(name);
          if (astNodeType != null) {
            if (astNodeType.isLeaf()) {
              ASTNode<?> leafNode = builder.build(name);
              makeRelation(visitor, parent, leafNode);
              parse(builder, visitor, parent, nextNode);
            } else {
              ASTNode<?> nonValNode = builder.build(name);
              parse(builder, visitor, nonValNode, nextNode);
              makeRelation(visitor, parent, nonValNode);
            }
            isObjectValueNode = false;
          }
        }
        if (isObjectValueNode) {
          ASTNode<?> subNode = builder.objectNode();
          parseJsonObjectNode(builder, visitor, subNode, subJsonNode);
          makeRelation(visitor, parent, subNode);
        }

      }
    }
  }

  static void parseJsonObjectNode(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent,
      JsonNode jsonNode) throws Exception {
    Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonNode> next = iterator.next();
      String name = next.getKey();
      JsonNode subJsonNode = next.getValue();
      ASTNodeType astNodeType = ASTNodeType.decideType(name);
      if (astNodeType != null) {
        if (astNodeType.isLeaf()) {
          ASTNode<?> leafNode = builder.build(name);
          makeRelation(visitor, parent, leafNode);
          parse(builder, visitor, parent, subJsonNode);
        } else {
          ASTNode<?> nonValNode = builder.build(name);
          parse(builder, visitor, nonValNode, subJsonNode);
          makeRelation(visitor, parent, nonValNode);
        }
      } else {
        ASTNode<?> entryNode = null;
        if (subJsonNode.isValueNode() || subJsonNode.isNull()) {
          entryNode = builder.entryNodeOf(name, createASTNodeFromJsonValue(builder, subJsonNode));
        } else if (subJsonNode.isArray()) {
          ASTNode<?> entryValueNode = builder.arrayNodeOf();
          parseJsonArrayNode(builder, visitor, entryValueNode, subJsonNode);
          entryNode = builder.entryNodeOf(name, entryValueNode);
        } else if (subJsonNode.isObject()) {
          ASTNode<?> entryValueNode = builder.objectNode();
          parseJsonObjectNode(builder, visitor, entryValueNode, subJsonNode);
          entryNode = builder.entryNodeOf(name, entryValueNode);
        }
        makeRelation(visitor, parent, entryNode);
      }
    }
  }

  static void parseJsonValueNode(ASTNodeBuilder builder, ASTNodeVisitor visitor, Node<?> parent,
      JsonNode jsonNode) throws IOException {
    ASTNode<?> node = createASTNodeFromJsonValue(builder, jsonNode);
    makeRelation(visitor, parent, node);
  }

}
