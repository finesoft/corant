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
package org.corant.modules.microprofile.jwt.jaxrs;

import static org.corant.context.Beans.find;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.enterprise.inject.Instance;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.microprofile.jwt.MpJWTAuthorizer;
import org.corant.modules.security.Authorizer;
import org.corant.modules.security.annotation.SecuredMetadata;
import org.corant.modules.security.annotation.SecuredType;
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimpleRoles;
import org.corant.modules.security.shared.interceptor.SecuredInterceptorCallback;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午12:29:58
 *
 */
@Priority(Priorities.AUTHORIZATION)
public class MpJWTAuthorizationFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final SecuredMetadata meta;
  private final SimplePermissions allowedPermits;
  private final SimpleRoles allowedRoles;
  volatile Authorizer authorizer;

  public MpJWTAuthorizationFilter(SecuredMetadata meta) {
    this.meta = meta;
    if (meta.denyAll()) {
      allowedPermits = null;
      allowedRoles = null;
    } else if (meta.type() == SecuredType.ROLE) {
      allowedRoles = SimpleRoles.of(meta.allowed());
      allowedPermits = null;
    } else {
      allowedPermits = SimplePermissions.of(meta.allowed());
      allowedRoles = null;
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    callbacks().forEachOrdered(cb -> cb.preSecuredIntercept(meta));
    Throwable throwable = null;
    try {
      if (meta.denyAll()) {
        throwable = new ForbiddenException();
      } else {
        Object roleOrPermit = defaultObject(allowedRoles, allowedPermits);
        authorizer().checkAccess(SecurityContexts.getCurrent(), roleOrPermit);
      }
    } catch (Exception e) {
      throwable = e;
    } finally {
      if (throwable != null) {
        requestContext.setProperty(MpJWTAuthenticationFilter.AUTHZ_EXCEPTION_KEY, throwable);
        if (requestContext.getSecurityContext().getUserPrincipal() == null) {
          Object ex = requestContext.getProperty(MpJWTAuthenticationFilter.AUTHC_EXCEPTION_KEY);
          if (ex instanceof Exception) {
            requestContext.removeProperty(MpJWTAuthenticationFilter.AUTHC_EXCEPTION_KEY);
            throwable.addSuppressed((Exception) ex);
          }
          throw new NotAuthorizedException(throwable, "Bearer");
        } else if (throwable instanceof ForbiddenException) {
          throw (ForbiddenException) throwable;
        } else {
          throw new ForbiddenException(throwable);
        }
      }
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) throws IOException {
    boolean success =
        requestContext.getProperty(MpJWTAuthenticationFilter.AUTHZ_EXCEPTION_KEY) == null
            && requestContext.getProperty(MpJWTAuthenticationFilter.AUTHC_EXCEPTION_KEY) == null;
    callbacks().forEachOrdered(cb -> cb.postSecuredIntercepted(success));
  }

  protected Authorizer authorizer() {
    if (authorizer == null) {
      synchronized (this) {
        if (authorizer == null) {
          authorizer = find(Authorizer.class).orElse(MpJWTAuthorizer.DFLT_INST);
        }
      }
    }
    return authorizer;
  }

  protected Stream<SecuredInterceptorCallback> callbacks() {
    Instance<SecuredInterceptorCallback> callback = select(SecuredInterceptorCallback.class);
    if (!callback.isUnsatisfied()) {
      return callback.stream().sorted(Sortable::compare);
    }
    return Stream.empty();
  }

}
