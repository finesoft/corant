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
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import javafx.collections.ObservableArray;

/**
 * corant-modules-javafx-jackson
 *
 * <p>
 * Uncompleted yet!!!!
 *
 * @author bingo 下午8:21:34
 *
 */
public class FXObservableArrayDeserializer<T extends ObservableArray<T>> extends StdDeserializer<T>
    implements ContextualDeserializer {

  private static final long serialVersionUID = 573536350963315614L;

  protected FXObservableArrayDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    return null;
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JacksonException {
    return null;
  }

}
