/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.conversion;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.Serializable;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 下午3:25:39
 */
public class ConverterType<S, T> implements Serializable {

  private static final long serialVersionUID = 4569587948510207966L;
  private final Class<S> sourceClass;
  private final Class<T> targetClass;
  private final int hash;

  /**
   * @param sourceClass the source object class
   * @param targetClass the specified converted type
   */
  public ConverterType(Class<S> sourceClass, Class<T> targetClass) {
    this.sourceClass = shouldNotNull(sourceClass);
    this.targetClass = shouldNotNull(targetClass);
    this.hash = Objects.hash(sourceClass, targetClass);
  }

  public static <S, T> ConverterType<S, T> of(Class<S> sourceClass, Class<T> targetClass) {
    return new ConverterType<>(sourceClass, targetClass);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    ConverterType other = (ConverterType) obj;
    if (sourceClass == null) {
      if (other.sourceClass != null) {
        return false;
      }
    } else if (!sourceClass.equals(other.sourceClass)) {
      return false;
    }
    if (targetClass == null) {
      return other.targetClass == null;
    } else {
      return targetClass.equals(other.targetClass);
    }
  }

  /**
   * @return the sourceClass
   */
  public Class<S> getSourceClass() {
    return sourceClass;
  }

  /**
   * @return the targetClass
   */
  public Class<T> getTargetClass() {
    return targetClass;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "ConverterType [sourceClass=" + sourceClass + ", targetClass=" + targetClass + "]";
  }

}
