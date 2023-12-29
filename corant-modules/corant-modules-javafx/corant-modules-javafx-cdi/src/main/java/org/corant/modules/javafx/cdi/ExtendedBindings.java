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

import java.util.stream.Collectors;
import org.corant.modules.javafx.cdi.ExtendedBiBinding.ExtendedBiBindingConverter;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 上午10:33:24
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

  public static <A, B> Object bindContentBi(ObservableList<A> list1, ObservableList<B> list2,
      ExtendedBiBindingConverter<A, B> converter) {
    ExtendedBiBinding.checkParameters(list1, list2);
    final ExtendedListContentBinding<A, B> binding =
        new ExtendedListContentBinding<>(list1, list2, converter);
    list1.setAll(list2.stream().map(converter::to).collect(Collectors.toList()));// FIXME
    list1.addListener(binding);
    list2.addListener(binding);
    return binding;
  }

  public static <A, B> Object bindContentBi(ObservableSet<A> set1, ObservableSet<B> set2,
      ExtendedBiBindingConverter<A, B> converter) {
    ExtendedBiBinding.checkParameters(set1, set2);
    final ExtendedSetContentBinding<A, B> binding =
        new ExtendedSetContentBinding<>(set1, set2, converter);
    set1.clear();
    set1.addAll(set2.stream().map(converter::to).collect(Collectors.toSet()));// FIXME
    set1.addListener(binding);
    set2.addListener(binding);
    return binding;
  }

  public static <A, B> void unBindBi(Property<A> propertyA, Property<B> propertyB) {
    ExtendedBiBinding.unbind(propertyA, propertyB);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void unBindContentBi(Object obj1, Object obj2) {
    ExtendedBiBinding.checkParameters(obj1, obj2);
    if ((obj1 instanceof ObservableList) && (obj2 instanceof ObservableList)) {
      final ObservableList list1 = (ObservableList) obj1;
      final ObservableList list2 = (ObservableList) obj2;
      final ExtendedListContentBinding binding = new ExtendedListContentBinding(list1, list2, null);
      list1.removeListener(binding);
      list2.removeListener(binding);
    } else if ((obj1 instanceof ObservableSet) && (obj2 instanceof ObservableSet)) {
      final ObservableSet set1 = (ObservableSet) obj1;
      final ObservableSet set2 = (ObservableSet) obj2;
      final ExtendedSetContentBinding binding = new ExtendedSetContentBinding(set1, set2, null);
      set1.removeListener(binding);
      set2.removeListener(binding);
    }
  }

}
