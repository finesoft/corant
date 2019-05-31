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
package org.corant.suites.jpa.shared.inject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * @author bingo 下午9:05:13
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface JPAProvider {

  String value();

  public static class JPAProviderLiteral extends AnnotationLiteral<JPAProvider>
      implements JPAProvider {

    private static final long serialVersionUID = -5552841006073177750L;

    private final String value;

    private JPAProviderLiteral(String value) {
      this.value = value;
    }

    public static JPAProviderLiteral[] from(String... values) {
      return Arrays.stream(values).map(JPAProviderLiteral::of).toArray(JPAProviderLiteral[]::new);
    }

    public static JPAProviderLiteral of(String value) {
      return new JPAProviderLiteral(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      JPAProviderLiteral other = (JPAProviderLiteral) obj;
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (value == null ? 0 : value.hashCode());
      return result;
    }

    @Override
    public String value() {
      return value;
    }
  }
}
