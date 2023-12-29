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
package org.corant.modules.javafx.jackson.databind;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * corant-modules-javafx-jackson
 * <p>
 * Class used to deserialize Javafx ObjectProperty objects, treating ObjectProperty objects type as
 * {@link com.fasterxml.jackson.databind.type.ReferenceType}.
 * <p>
 * Note: This is experimental and not recommended for production environments, if you have a better
 * implementation, please contact me <a href="mailto:finesoft@gmail.com">finesoft@gmail.com</a>.
 *
 * @author bingo 上午10:11:02
 */
public class FXObjectPropertyDeserializer
    extends ReferenceTypeDeserializer<ObjectProperty<Object>> {

  private static final long serialVersionUID = -6593896229115419704L;

  public FXObjectPropertyDeserializer(JavaType fullType, ValueInstantiator inst,
      TypeDeserializer typeDeser, JsonDeserializer<?> deser) {
    super(fullType, inst, typeDeser, deser);
  }

  @Override
  public Object getAbsentValue(DeserializationContext ctxt) throws JsonMappingException {
    return null;
  }

  @Override
  public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
    return getNullValue(ctxt);
  }

  @Override
  public ObjectProperty<Object> getNullValue(DeserializationContext ctxt)
      throws JsonMappingException {
    return new SimpleObjectProperty<>(_valueDeserializer.getNullValue(ctxt));
  }

  @Override
  public Object getReferenced(ObjectProperty<Object> reference) {
    return reference.get();
  }

  @Override
  public ObjectProperty<Object> referenceValue(Object contents) {
    return new SimpleObjectProperty<>(contents);
  }

  @Override
  public Boolean supportsUpdate(DeserializationConfig config) {
    return Boolean.TRUE;
  }

  @Override
  public ObjectProperty<Object> updateReference(ObjectProperty<Object> reference, Object contents) {
    reference.set(contents);
    return reference;
  }

  @Override
  public FXObjectPropertyDeserializer withResolved(TypeDeserializer typeDeser,
      JsonDeserializer<?> valueDeser) {
    return new FXObjectPropertyDeserializer(_fullType, _valueInstantiator, typeDeser, valueDeser);
  }
}
