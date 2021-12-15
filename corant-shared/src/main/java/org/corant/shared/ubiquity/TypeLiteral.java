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
package org.corant.shared.ubiquity;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 * <p>
 * Extract from javax.enterprise.util.TypeLiteral, and discard the implementation of Serializable
 * interfaces.
 *
 * @author bingo 下午6:17:53
 *
 */
public abstract class TypeLiteral<T> {

  public static final Type[] EMPTY_ARRAY = {};

  private transient Type actualType;

  protected TypeLiteral() {}

  private static Class<?> getTypeLiteralSubclass(Class<?> clazz) {
    Class<?> superclass = clazz.getSuperclass();
    if (superclass.equals(TypeLiteral.class)) {
      return clazz;
    } else if (superclass.equals(Object.class)) {
      return null;
    } else {
      return getTypeLiteralSubclass(superclass);
    }
  }

  private static Type getTypeParameter(Class<?> superclass) {
    Type type = superclass.getGenericSuperclass();
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (parameterizedType.getActualTypeArguments().length == 1) {
        return parameterizedType.getActualTypeArguments()[0];
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TypeLiteral<?>) {
      TypeLiteral<?> that = (TypeLiteral<?>) obj;
      return this.getType().equals(that.getType());
    }
    return false;
  }

  /**
   * @return the raw type represented by this object
   */
  @SuppressWarnings("unchecked")
  public final Class<T> getRawType() {
    Type type = getType();
    if (type instanceof Class) {
      return (Class<T>) type;
    } else if (type instanceof ParameterizedType) {
      return (Class<T>) ((ParameterizedType) type).getRawType();
    } else if (type instanceof GenericArrayType) {
      return (Class<T>) Object[].class;
    } else {
      throw new CorantRuntimeException("Illegal type");
    }
  }

  /**
   * @return the actual type represented by this object
   */
  public final Type getType() {
    if (actualType == null) {
      Class<?> typeLiteralSubclass = getTypeLiteralSubclass(this.getClass());
      if (typeLiteralSubclass == null) {
        throw new CorantRuntimeException(getClass() + " is not a subclass of TypeLiteral");
      }
      actualType = getTypeParameter(typeLiteralSubclass);
      if (actualType == null) {
        throw new CorantRuntimeException(
            getClass() + " does not specify the type parameter T of TypeLiteral<T>");
      }
    }
    return actualType;
  }

  @Override
  public int hashCode() {
    return getType().hashCode();
  }

  @Override
  public String toString() {
    return getType().toString();
  }

}
