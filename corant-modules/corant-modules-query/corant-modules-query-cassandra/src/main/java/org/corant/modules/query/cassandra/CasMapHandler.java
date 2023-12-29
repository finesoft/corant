/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.cassandra;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.GettableData;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;
import com.google.common.reflect.TypeToken;

/**
 * corant-modules-query-cassandra
 *
 * @author bingo 上午11:40:06
 */
public class CasMapHandler {

  static final CodecRegistry CODE_REGISTRY = CodecRegistry.DEFAULT_INSTANCE;

  public static List<Map<Object, Object>> get(ResultSet resultSet) {
    return resultSet.all().stream().map(CasMapHandler::get).collect(Collectors.toList());
  }

  public static Map<Object, Object> get(Row row) {
    Map<Object, Object> rowMap = new LinkedHashMap<>();
    for (ColumnDefinitions.Definition definition : row.getColumnDefinitions().asList()) {
      String name = definition.getName();
      rowMap.put(name, get(name, definition.getType(), row));
    }
    return rowMap;
  }

  public static Object get(String name, DataType dataType, GettableData row) {
    switch (dataType.getName()) {
      case LIST:
        DataType typeList = dataType.getTypeArguments().get(0);
        TypeToken<Object> javaTypeList = CODE_REGISTRY.codecFor(typeList).getJavaType();
        return row.getList(name, javaTypeList).stream().map(CasMapHandler::convertValue)
            .collect(Collectors.toList());
      case SET:
        DataType typeSet = dataType.getTypeArguments().get(0);
        TypeToken<Object> javaTypeSet = CODE_REGISTRY.codecFor(typeSet).getJavaType();
        return row.getSet(name, javaTypeSet).stream().map(CasMapHandler::convertValue)
            .collect(Collectors.toSet());
      case MAP:
        DataType typeKey = dataType.getTypeArguments().get(0);
        DataType typeValue = dataType.getTypeArguments().get(1);
        TypeToken<Object> javaTypeKey = CODE_REGISTRY.codecFor(typeKey).getJavaType();
        TypeToken<Object> javaTypeValue = CODE_REGISTRY.codecFor(typeValue).getJavaType();
        Map<Object, Object> map = new LinkedHashMap<>();
        row.getMap(name, javaTypeKey, javaTypeValue)
            .forEach((k, v) -> map.put(convertValue(k), convertValue(v)));
        return map;
      case UDT:
        UDTValue udtValue = row.getUDTValue(name);
        return getUDT(udtValue);
      default:
        TypeCodec<Object> objectTypeCodec = CODE_REGISTRY.codecFor(dataType);
        return row.get(name, objectTypeCodec);
    }

  }

  static Object convertValue(Object value) {
    if (value instanceof UDTValue) {
      return getUDT((UDTValue) value);
    } else {
      return value;
    }
  }

  static Object getUDT(UDTValue udtValue) {
    Map<Object, Object> map = new LinkedHashMap<>();
    UserType type = udtValue.getType();
    for (String fieldName : type.getFieldNames()) {
      DataType fieldType = type.getFieldType(fieldName);
      Object elementValue =
          convertValue(udtValue.get(fieldName, CODE_REGISTRY.codecFor(fieldType)));
      map.put(fieldName, elementValue);
    }
    return map;
  }

}
