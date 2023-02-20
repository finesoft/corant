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

import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import javafx.beans.property.ObjectProperty;

/**
 * corant-modules-javafx-jackson
 *
 * @author bingo 上午11:13:48
 *
 */
public class FXTypeModifier extends TypeModifier {

  @Override
  public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context,
      TypeFactory typeFactory) {
    if (type.isReferenceType() || type.isContainerType()) {
      return type;
    }
    if (type.getRawClass() == ObjectProperty.class) {
      return ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0));
    } else {
      return type;
    }
  }

}
