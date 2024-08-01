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
package org.corant.context.qualifier;

import static org.corant.shared.util.Annotations.calculateMembersHashCode;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-context
 *
 * @author bingo 下午8:44:38
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface SURI {

  SURILiteral INSTANCE = new SURILiteral(EMPTY);

  @Nonbinding
  String value() default EMPTY;

  class SURILiteral extends AnnotationLiteral<SURI> implements SURI {

    private static final long serialVersionUID = 4186590008857391708L;

    private String value;
    private transient volatile Integer hashCode;

    private SURILiteral(String value) {
      this.value = defaultString(value);
    }

    public static SURILiteral of(String value) {
      return new SURILiteral(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !SURI.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      SURI other = (SURI) obj;
      return value.equals(other.value());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("value", value));
      }
      return hashCode;
    }

    @Override
    public String value() {
      return value;
    }
  }
}
