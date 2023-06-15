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
package org.corant.modules.javafx.cdi;

import java.lang.ref.WeakReference;
import org.corant.modules.javafx.cdi.ExtendedBiBinding.ExtendedBiBindingConverter;
import org.corant.shared.util.Lists;
import javafx.beans.WeakListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class ExtendedListContentBinding<A, B> implements ListChangeListener<Object>, WeakListener {

  private final WeakReference<ObservableList<A>> propertyRef1;
  private final WeakReference<ObservableList<B>> propertyRef2;
  private final WeakReference<ExtendedBiBindingConverter<A, B>> converterRef;

  private boolean updating = false;

  public ExtendedListContentBinding(ObservableList<A> list1, ObservableList<B> list2,
      ExtendedBiBindingConverter<A, B> converter) {
    propertyRef1 = new WeakReference<>(list1);
    propertyRef2 = new WeakReference<>(list2);
    converterRef = new WeakReference<>(converter);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    final Object propertyA1 = propertyRef1.get();
    final Object propertyA2 = propertyRef2.get();
    if ((propertyA1 == null) || (propertyA2 == null)) {
      return false;
    }

    if (obj instanceof ExtendedListContentBinding) {
      final ExtendedListContentBinding otherBinding = (ExtendedListContentBinding) obj;
      final Object propertyB1 = otherBinding.propertyRef1.get();
      final Object propertyB2 = otherBinding.propertyRef2.get();
      if ((propertyB1 == null) || (propertyB2 == null)) {
        return false;
      }

      if ((propertyA1 == propertyB1) && (propertyA2 == propertyB2)) {
        return true;
      }
      if ((propertyA1 == propertyB2) && (propertyA2 == propertyB1)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    final ObservableList<A> list1 = propertyRef1.get();
    final ObservableList<B> list2 = propertyRef2.get();
    final int hc1 = (list1 == null) ? 0 : list1.hashCode();
    final int hc2 = (list2 == null) ? 0 : list2.hashCode();
    return hc1 * hc2;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onChanged(Change<?> change) {
    if (!updating) {
      final ObservableList<A> list1 = propertyRef1.get();
      final ObservableList<B> list2 = propertyRef2.get();
      if ((list1 == null) || (list2 == null)) {
        if (list1 != null) {
          list1.removeListener(this);
        }
        if (list2 != null) {
          list2.removeListener(this);
        }
      } else {
        try {
          updating = true;
          if (list1 == change.getList()) {
            final ObservableList<B> dest = list2;
            final Change<A> changes = (Change<A>) change;
            while (changes.next()) {
              if (changes.wasPermutated()) {
                dest.remove(changes.getFrom(), changes.getTo());
                dest.addAll(changes.getFrom(),
                    Lists.transform(changes.getList().subList(changes.getFrom(), changes.getTo()),
                        converterRef.get()::from));
              } else {
                if (changes.wasRemoved()) {
                  dest.remove(changes.getFrom(), changes.getFrom() + changes.getRemovedSize());
                }
                if (changes.wasAdded()) {
                  dest.addAll(changes.getFrom(),
                      Lists.transform(changes.getAddedSubList(), converterRef.get()::from));
                }
              }
            }
          } else {
            final ObservableList<A> dest = list1;
            final Change<B> changes = (Change<B>) change;
            while (changes.next()) {
              if (changes.wasPermutated()) {
                dest.remove(changes.getFrom(), changes.getTo());
                dest.addAll(changes.getFrom(),
                    Lists.transform(changes.getList().subList(changes.getFrom(), changes.getTo()),
                        converterRef.get()::to));
              } else {
                if (changes.wasRemoved()) {
                  dest.remove(changes.getFrom(), changes.getFrom() + changes.getRemovedSize());
                }
                if (changes.wasAdded()) {
                  dest.addAll(changes.getFrom(),
                      Lists.transform(changes.getAddedSubList(), converterRef.get()::to));
                }
              }
            }
          }
        } finally {
          updating = false;
        }
      }
    }
  }

  @Override
  public boolean wasGarbageCollected() {
    return (propertyRef1.get() == null) || (propertyRef2.get() == null);
  }
}
