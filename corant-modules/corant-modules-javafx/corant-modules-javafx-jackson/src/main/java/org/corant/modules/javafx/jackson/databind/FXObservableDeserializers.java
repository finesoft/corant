/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.javafx.jackson.databind;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

/**
 * corant-modules-javafx-jackson
 *
 * @author bingo 下午9:36:44
 *
 */
public class FXObservableDeserializers extends Deserializers.Base {

  @Override
  public JsonDeserializer<?> findCollectionDeserializer(CollectionType type,
      DeserializationConfig config, BeanDescription beanDesc,
      TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
      throws JsonMappingException {
    Class<?> raw = type.getRawClass();
    if (ObservableList.class.isAssignableFrom(raw)) {
      return new FXObservableListDeserializer<>(type, elementTypeDeserializer, elementDeserializer);
    }
    if (ObservableSet.class.isAssignableFrom(raw)) {
      return new FXObservableSetDeserializer<>(type, elementTypeDeserializer, elementDeserializer);
    }
    return null;
  }

  @Override
  public JsonDeserializer<?> findMapDeserializer(MapType type, DeserializationConfig config,
      BeanDescription beanDesc, KeyDeserializer keyDeserializer,
      TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
      throws JsonMappingException {
    Class<?> raw = type.getRawClass();
    if (ObservableMap.class.isAssignableFrom(raw)) {
      return new FXObservableMapDeserializer<>(type, keyDeserializer, elementTypeDeserializer,
          elementDeserializer);
    }
    return null;
  }

  @Override
  public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
      DeserializationConfig config, BeanDescription beanDesc,
      TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
      throws JsonMappingException {
    if (refType.isTypeOrSubTypeOf(ObjectProperty.class)) {
      Class<?> raw = refType.getRawClass();
      if (raw == ObjectProperty.class) {
        return new FXObjectPropertyDeserializer(refType, null, contentTypeDeserializer,
            contentDeserializer);
      }
    }
    return null;
  }
}
