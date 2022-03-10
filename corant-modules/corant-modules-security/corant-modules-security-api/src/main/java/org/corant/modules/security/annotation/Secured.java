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
import static org.corant.shared.util.Lists.appendIfAbsent;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.EMPTY_ARRAY;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
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

  String ALLOWED_ALL_SIGN = Strings.ASTERISK;

  @Nonbinding
  String[] allowed() default {};

  @Nonbinding
  String runAs() default EMPTY;

  @Nonbinding
  String type() default EMPTY;

  class SecuredLiteral extends AnnotationLiteral<Secured> implements Secured {

    private static final long serialVersionUID = -2874685620910996061L;

    String[] allowed;

    String type;

    String runAs;

    public SecuredLiteral(String type, String runAs, String[] allowed) {
      this.type = type;
      this.runAs = runAs;
      this.allowed = allowed == null ? EMPTY_ARRAY : Arrays.copyOf(allowed, allowed.length);
    }

    public static Secured of(PermitAll permitAll) {
      return new SecuredLiteral(SecuredType.ROLE.name(), EMPTY, EMPTY_ARRAY);
    }

    public static Secured of(RunAs runas) {
      return new SecuredLiteral(SecuredType.ROLE.name(), runas.value(), Strings.EMPTY_ARRAY);
    }

    public static Secured of(Secured secured) {
      return secured == null ? null
          : new SecuredLiteral(secured.type(), secured.runAs(), secured.allowed());
    }

    public static Secured of(Set<RolesAllowed> rolesAlloweds) {
      String[] roles = Strings.EMPTY_ARRAY;
      for (RolesAllowed r : rolesAlloweds) {
        roles = appendIfAbsent(roles, r.value());
      }
      return new SecuredLiteral(SecuredType.ROLE.name(), Strings.EMPTY, roles);
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
    public String type() {
      return type;
    }

  }
}
