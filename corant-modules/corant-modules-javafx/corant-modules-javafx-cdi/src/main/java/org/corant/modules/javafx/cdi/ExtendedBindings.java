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
package org.corant.modules.javafx.cdi;

import org.corant.modules.javafx.cdi.ExtendedBiBinding.ExtendedBiBindingConverter;
import javafx.beans.property.Property;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 上午10:33:24
 *
 */
public class ExtendedBindings {

  private ExtendedBindings() {}

  public static <A, B> ExtendedBiBinding<A, B> bindBi(Property<A> propertyA, Property<B> propertyB,
      ExtendedBiBindingConverter<A, B> converter) {
    final ExtendedBiBinding<A, B> binding =
        new ExtendedBiBinding<>(propertyA, propertyB, converter);
    propertyA.setValue(converter.to(propertyB.getValue()));
    propertyA.addListener(binding);
    propertyB.addListener(binding);
    return binding;
  }

  public static <A, B> void unBindBi(Property<A> propertyA, Property<B> propertyB) {
    ExtendedBiBinding.unbind(propertyA, propertyB);
  }

}
