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
import java.util.Set;
import org.corant.modules.javafx.cdi.ExtendedBiBinding.ExtendedBiBindingConverter;
import javafx.beans.WeakListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

public class ExtendedSetContentBinding<A, B> implements SetChangeListener<Object>, WeakListener {

  private final WeakReference<ObservableSet<A>> propertyRef1;
  private final WeakReference<ObservableSet<B>> propertyRef2;
  private final WeakReference<ExtendedBiBindingConverter<A, B>> converterRef;

  private boolean updating = false;

  public ExtendedSetContentBinding(ObservableSet<A> list1, ObservableSet<B> list2,
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

    if (obj instanceof ExtendedSetContentBinding) {
      final ExtendedSetContentBinding otherBinding = (ExtendedSetContentBinding) obj;
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
    final ObservableSet<A> set1 = propertyRef1.get();
    final ObservableSet<B> set2 = propertyRef2.get();
    final int hc1 = (set1 == null) ? 0 : set1.hashCode();
    final int hc2 = (set2 == null) ? 0 : set2.hashCode();
    return hc1 * hc2;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onChanged(Change<?> change) {
    if (!updating) {
      final ObservableSet<A> set1 = propertyRef1.get();
      final ObservableSet<B> set2 = propertyRef2.get();
      if ((set1 == null) || (set2 == null)) {
        if (set1 != null) {
          set1.removeListener(this);
        }
        if (set2 != null) {
          set2.removeListener(this);
        }
      } else {
        try {
          updating = true;
          if (set1 == change.getSet()) {
            final Change<A> changes = (Change<A>) change;
            final Set<B> dest = set2;
            if (changes.wasRemoved()) {
              dest.remove(converterRef.get().from(changes.getElementRemoved()));
            } else {
              dest.add(converterRef.get().from(changes.getElementAdded()));
            }
          } else {
            final Change<B> changes = (Change<B>) change;
            final Set<A> dest = set1;
            if (changes.wasRemoved()) {
              dest.remove(converterRef.get().to(changes.getElementRemoved()));
            } else {
              dest.add(converterRef.get().to(changes.getElementAdded()));
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
