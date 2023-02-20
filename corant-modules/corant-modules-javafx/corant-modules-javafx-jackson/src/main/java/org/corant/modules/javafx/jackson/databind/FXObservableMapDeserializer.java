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
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.type.MapType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * corant-modules-javafx-jackson
 *
 * <p>
 * Class used to deserialize Javafx ObservableMap objects.
 * <p>
 * Note: This is experimental and not recommended for production environments, if you have a better
 * implementation, please contact me <a href="mailto:finesoft@gmail.com">finesoft@gmail.com</a>.
 *
 * @author bingo 下午9:39:20
 *
 */
@SuppressWarnings("unchecked")
public class FXObservableMapDeserializer<T extends ObservableMap<Object, Object>>
    extends JsonDeserializer<T> implements ContextualDeserializer {

  protected final MapType _mapType;

  protected KeyDeserializer _keyDeserializer;

  protected JsonDeserializer<?> _valueDeserializer;

  protected final TypeDeserializer _typeDeserializerForValue;

  protected FXObservableMapDeserializer(MapType type, KeyDeserializer keyDeser,
      TypeDeserializer typeDeser, JsonDeserializer<?> deser) {
    _mapType = type;
    _keyDeserializer = keyDeser;
    _typeDeserializerForValue = typeDeser;
    _valueDeserializer = deser;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    KeyDeserializer keyDeser = _keyDeserializer;
    JsonDeserializer<?> deser = _valueDeserializer;
    TypeDeserializer typeDeser = _typeDeserializerForValue;
    if ((keyDeser != null) && (deser != null) && (typeDeser == null)) { // nope
      return this;
    }
    if (keyDeser == null) {
      keyDeser = ctxt.findKeyDeserializer(_mapType.getKeyType(), property);
    }
    if (deser == null) {
      deser = ctxt.findContextualValueDeserializer(_mapType.getContentType(), property);
    }
    if (typeDeser != null) {
      typeDeser = typeDeser.forProperty(property);
    }
    return withResolved(keyDeser, typeDeser, deser);
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken t = p.getCurrentToken();
    if (t == JsonToken.START_OBJECT) {
      t = p.nextToken();
    }
    if (t != JsonToken.FIELD_NAME && t != JsonToken.END_OBJECT) {
      return (T) ctxt.handleUnexpectedToken(handledType(), p);
    }
    return _deserializeEntries(p, ctxt);
  }

  @Override
  public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
      TypeDeserializer typeDeserializer) throws IOException {
    return typeDeserializer.deserializeTypedFromObject(p, ctxt);
  }

  @Override
  public LogicalType logicalType() {
    return LogicalType.Map;
  }

  public FXObservableMapDeserializer<T> withResolved(KeyDeserializer keyDeser,
      TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    return new FXObservableMapDeserializer<>(_mapType, keyDeser, typeDeser, valueDeser);
  }

  protected T _deserializeEntries(JsonParser p, DeserializationContext ctxt) throws IOException {
    final KeyDeserializer keyDes = _keyDeserializer;
    final JsonDeserializer<?> valueDes = _valueDeserializer;
    final TypeDeserializer typeDeser = _typeDeserializerForValue;

    T map = createEmptyMap();
    for (; p.getCurrentToken() == JsonToken.FIELD_NAME; p.nextToken()) {
      String fieldName = p.getCurrentName();
      Object key = (keyDes == null) ? fieldName : keyDes.deserializeKey(fieldName, ctxt);
      JsonToken t = p.nextToken();
      Object value;
      if (t == JsonToken.VALUE_NULL) {
        map = _handleNull(ctxt, key, _valueDeserializer, map);
        continue;
      }
      if (typeDeser == null) {
        value = valueDes.deserialize(p, ctxt);
      } else {
        value = valueDes.deserializeWithType(p, ctxt, typeDeser);
      }
      map.put(key, value);
    }
    return map;
  }

  protected T _handleNull(DeserializationContext ctxt, Object key, JsonDeserializer<?> valueDeser,
      T map) throws IOException {
    Object nvl = valueDeser.getNullValue(ctxt);
    if (nvl != null) {
      return (T) map.put(key, nvl);
    } else {
      return map;
    }
  }

  protected T createEmptyMap() {
    return (T) FXCollections.observableHashMap();
  }
}
