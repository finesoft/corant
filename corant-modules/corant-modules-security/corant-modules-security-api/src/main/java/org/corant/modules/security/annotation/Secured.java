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
package org.corant.modules.security.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import org.corant.shared.util.Strings;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午1:49:51
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, CONSTRUCTOR})
@InterceptorBinding
@Inherited
public @interface Secured {

  @Nonbinding
  String[] allowed() default {};

  @Nonbinding
  String runAs() default EMPTY;

  @Nonbinding
  SecuredType type() default SecuredType.ROLE;

  class SecureLiteral extends AnnotationLiteral<Secured> implements Secured {

    private static final long serialVersionUID = -2874685620910996061L;

    String[] allowed;

    SecuredType type;

    String runAs;

    public SecureLiteral(SecuredType type, String runAs, String[] allowed) {
      this.type = defaultObject(type, SecuredType.ROLE);
      this.runAs = defaultString(runAs);
      this.allowed = defaultObject(allowed, Strings.EMPTY_ARRAY);
    }

    @Override
    public String[] allowed() {
      return allowed;
    }

    @Override
    public String runAs() {
      return runAs;
    }

    @Override
    public SecuredType type() {
      return type;
    }

  }
}
