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

import java.lang.ref.WeakReference;
import javafx.beans.WeakListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午7:24:16
 *
 */
public class ExtendedBiBinding<A, B> implements ChangeListener<Object>, WeakListener {
  private final int cachedHashCode;
  private boolean updating;
  private final WeakReference<Property<A>> propertyRefA;
  private final WeakReference<Property<B>> propertyRefB;
  private final ExtendedBiBinding.ExtendedBiBindingConverter<A, B> converter;

  public ExtendedBiBinding(Property<A> propertyA, Property<B> propertyB,
      ExtendedBiBinding.ExtendedBiBindingConverter<A, B> converter) {
    checkParameters(propertyA, propertyB);
    cachedHashCode = propertyA.hashCode() * propertyB.hashCode();
    propertyRefA = new WeakReference<>(propertyA);
    propertyRefB = new WeakReference<>(propertyB);
    this.converter = converter;
  }

  static void checkParameters(Object propertyA, Object propertyB) {
    if ((propertyA == null) || (propertyB == null)) {
      throw new NullPointerException("Both properties must be specified.");
    }
    if (propertyA == propertyB) {
      throw new IllegalArgumentException("Cannot bind property to itself");
    }
  }

  static <T, B> void unbind(Property<T> propertyA, Property<B> propertyB) {
    final ExtendedBiBinding<T, B> binding =
        new ExtendedBiBinding.ExtendedBiUnBinding<>(propertyA, propertyB, null);
    propertyA.removeListener(binding);
    propertyB.removeListener(binding);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void changed(ObservableValue<? extends Object> sourceProperty, Object oldValue,
      Object newValue) {
    if (!updating) {
      final Property<A> propertyA = propertyRefA.get();
      final Property<B> propertyB = propertyRefB.get();
      if ((propertyA == null) || (propertyB == null)) {
        if (propertyA != null) {
          propertyA.removeListener(this);
        }
        if (propertyB != null) {
          propertyB.removeListener(this);
        }
      } else {
        try {
          updating = true;
          if (propertyA == sourceProperty) {
            propertyB.setValue(converter.from((A) newValue));
          } else {
            propertyA.setValue(converter.to((B) newValue));
          }
        } catch (RuntimeException e) {
          try {
            if (propertyA == sourceProperty) {
              propertyA.setValue((A) oldValue);
            } else {
              propertyB.setValue((B) oldValue);
            }
          } catch (Exception e2) {
            e2.addSuppressed(e);
            unbind(propertyA, propertyB);
            throw new RuntimeException("Bidirectional binding failed together with an attempt"
                + " to restore the source property to the previous value."
                + " Removing the bidirectional binding from properties " + propertyA + " and "
                + propertyB, e2);
          }
          throw new RuntimeException("Bidirectional binding failed, setting to the previous value",
              e);
        } finally {
          updating = false;
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    final Object propertyA1 = getPropertyA();
    final Object propertyB1 = getPropertyB();
    if ((propertyA1 == null) || (propertyB1 == null)) {
      return false;
    }

    if (obj instanceof ExtendedBiBinding) {
      final ExtendedBiBinding otherBinding = (ExtendedBiBinding) obj;
      final Object propertyA2 = otherBinding.getPropertyA();
      final Object propertyB2 = otherBinding.getPropertyB();
      if ((propertyA2 == null) || (propertyB2 == null)) {
        return false;
      }

      if (propertyA1 == propertyA2 && propertyB1 == propertyB2) {
        return true;
      }
      if (propertyA1 == propertyB2 && propertyB1 == propertyA2) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return cachedHashCode;
  }

  @Override
  public boolean wasGarbageCollected() {
    return (getPropertyA() == null) || (getPropertyB() == null);
  }

  protected Property<A> getPropertyA() {
    return propertyRefA.get();
  }

  protected Property<B> getPropertyB() {
    return propertyRefB.get();
  }

  /**
   * corant-modules-javafx-cdi
   *
   * @author bingo 下午7:43:51
   *
   */
  public interface ExtendedBiBindingConverter<T, B> {

    B from(T obj);

    T to(B obj);
  }

  /**
   * corant-modules-javafx-cdi
   *
   * @author bingo 下午8:11:24
   *
   */
  public static class ExtendedBiUnBinding<T, B> extends ExtendedBiBinding<T, B> {

    public ExtendedBiUnBinding(Property<T> property1, Property<B> property2,
        ExtendedBiBinding.ExtendedBiBindingConverter<T, B> converter) {
      super(property1, property2, converter);
    }

    @Override
    public void changed(ObservableValue<? extends Object> sourceProperty, Object oldValue,
        Object newValue) {
      throw new RuntimeException("Should not reach here");
    }
  }
}
