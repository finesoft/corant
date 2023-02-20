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

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.LogicalType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * corant-modules-javafx-jackson
 *
 * <p>
 * Class used to deserialize Javafx ObservableSet objects.
 * <p>
 * Note: This is experimental and not recommended for production environments, if you have a better
 * implementation, please contact me <a href="mailto:finesoft@gmail.com">finesoft@gmail.com</a>.
 *
 * @author bingo 下午9:39:30
 *
 */
@SuppressWarnings("unchecked")
public class FXObservableSetDeserializer<T extends ObservableSet<Object>> extends StdDeserializer<T>
    implements ContextualDeserializer {

  private static final long serialVersionUID = 6937944698047504921L;

  protected final CollectionType _containerType;

  protected final JsonDeserializer<?> _valueDeserializer;

  protected final TypeDeserializer _typeDeserializerForValue;

  protected FXObservableSetDeserializer(CollectionType type, TypeDeserializer typeDeser,
      JsonDeserializer<?> deser) {
    super(type);
    _containerType = type;
    _typeDeserializerForValue = typeDeser;
    _valueDeserializer = deser;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    JsonDeserializer<?> deser = _valueDeserializer;
    TypeDeserializer typeDeser = _typeDeserializerForValue;
    if (deser == null) {
      deser = ctxt.findContextualValueDeserializer(_containerType.getContentType(), property);
    }
    if (typeDeser != null) {
      typeDeser = typeDeser.forProperty(property);
    }
    if (deser == _valueDeserializer && typeDeser == _typeDeserializerForValue) {
      return this;
    }
    return withResolved(typeDeser, deser);
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.isExpectedStartArrayToken()) {
      return _deserializeContents(p, ctxt);
    }
    if (ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
      return _deserializeFromSingleValue(p, ctxt);
    }
    return (T) ctxt.handleUnexpectedToken(handledType(), p);
  }

  @Override
  public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
      TypeDeserializer typeDeserializer) throws IOException {
    return typeDeserializer.deserializeTypedFromArray(p, ctxt);
  }

  @Override
  public LogicalType logicalType() {
    return LogicalType.Collection;
  }

  public FXObservableSetDeserializer<T> withResolved(TypeDeserializer typeDeser,
      JsonDeserializer<?> valueDeser) {
    return new FXObservableSetDeserializer<>(_containerType, typeDeser, valueDeser);
  }

  protected T _deserializeContents(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonDeserializer<?> valueDes = _valueDeserializer;
    JsonToken t;
    final TypeDeserializer typeDeser = _typeDeserializerForValue;
    T collection = createEmptyCollection();

    while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
      Object value;

      if (t == JsonToken.VALUE_NULL) {
        value = null;
      } else if (typeDeser == null) {
        value = valueDes.deserialize(p, ctxt);
      } else {
        value = valueDes.deserializeWithType(p, ctxt, typeDeser);
      }
      collection.add(value);
    }
    return collection;
  }

  protected T _deserializeFromSingleValue(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonDeserializer<?> valueDes = _valueDeserializer;
    final TypeDeserializer typeDeser = _typeDeserializerForValue;
    JsonToken t = p.getCurrentToken();

    Object value;

    if (t == JsonToken.VALUE_NULL) {
      value = null;
    } else if (typeDeser == null) {
      value = valueDes.deserialize(p, ctxt);
    } else {
      value = valueDes.deserializeWithType(p, ctxt, typeDeser);
    }
    T collection = createEmptyCollection();
    collection.add(value);
    return collection;
  }

  protected T createEmptyCollection() {
    return (T) FXCollections.observableSet();
  }
}
