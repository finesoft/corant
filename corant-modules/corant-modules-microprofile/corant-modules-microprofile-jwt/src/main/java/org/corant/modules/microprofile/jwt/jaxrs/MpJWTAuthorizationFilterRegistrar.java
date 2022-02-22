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
package org.corant.modules.microprofile.jwt.jaxrs;

import static org.corant.shared.util.Sets.setOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.Secured.SecuredLiteral;
import org.corant.modules.security.annotation.SecuredType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Strings;
import io.smallrye.jwt.auth.jaxrs.DenyAllFilter;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午6:06:58
 *
 */
public class MpJWTAuthorizationFilterRegistrar implements DynamicFeature {

  private static final DenyAllFilter denyAllFilter = new DenyAllFilter();
  private static final Map<ResourceInfo, Consumer<FeatureContext>> handlers =
      new ConcurrentHashMap<>();// static?
  private static final Set<Class<? extends Annotation>> mpJwtAnnotations =
      setOf(DenyAll.class, PermitAll.class, RolesAllowed.class, Secured.class);

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    handlers.computeIfAbsent(MpResourceInfo.of(resourceInfo), ri -> new Consumer<>() {
      private final Object registration = resolveRegistration(ri);

      @Override
      public void accept(FeatureContext featureContext) {
        if (registration != null) {
          featureContext.register(registration);
        }
      }
    }).accept(context);
  }

  boolean hasSecurityAnnotations(ResourceInfo resource) {
    // resource methods are inherited (see JAX-RS spec, chapter 3.6)
    // resource methods must be `public` (see JAX-RS spec, chapter 3.3.1)
    // hence `resourceClass.getMethods` -- returns public methods, including inherited ones
    return Stream.of(resource.getResourceClass().getMethods()).filter(this::isResourceMethod)
        .anyMatch(this::hasSecurityAnnotations);
  }

  Object resolveRegistration(ResourceInfo resourceInfo) {
    Object registration = null;
    Annotation mpJwtAnnotation = getMpJwtAnnotation(resourceInfo);
    if (mpJwtAnnotation != null) {
      if (mpJwtAnnotation instanceof DenyAll) {
        registration = denyAllFilter;
      } else if (mpJwtAnnotation instanceof RolesAllowed) {
        registration = new MpJWTRolesAllowedFilter(((RolesAllowed) mpJwtAnnotation).value());
      } else if (mpJwtAnnotation instanceof Secured) {
        Secured secured = SecuredLiteral.of((Secured) mpJwtAnnotation);
        if (SecuredType.valueOf(secured.type()) == SecuredType.PERMIT) {
          registration = new MpJWTPermitsAllowedFilter(secured.allowed());
        } else {
          registration = new MpJWTRolesAllowedFilter(secured.allowed());
        }
      } else if (mpJwtAnnotation instanceof PermitAll) {
        registration = new MpJWTRolesAllowedFilter(Strings.EMPTY_ARRAY);
      }
    } else if (hasSecurityAnnotations(resourceInfo) && shouldNonannotatedMethodsBeDenied()) {
      registration = denyAllFilter;
    }
    return registration;
  }

  boolean shouldNonannotatedMethodsBeDenied() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL resource = loader.getResource("/META-INF/MP-JWT-DENY-NONANNOTATED-METHODS");
    return resource != null;
  }

  private Annotation getAnnotation(Annotation[] declaredAnnotations,
      Supplier<String> annotationPlacementDescriptor) {
    List<Annotation> annotations = Stream.of(declaredAnnotations)
        .filter(annotation -> mpJwtAnnotations.contains(annotation.annotationType()))
        .collect(Collectors.toList());
    switch (annotations.size()) {
      case 0:
        return null;
      case 1:
        return annotations.iterator().next();
      default:
        throw new CorantRuntimeException(
            "Duplicate MicroProfile JWT annotations found on " + annotationPlacementDescriptor.get()
                + ". Expected at most 1 annotation, found: " + annotations);
    }
  }

  private Annotation getMpJwtAnnotation(ResourceInfo resourceInfo) {
    Annotation annotation = getAnnotation(resourceInfo.getResourceMethod().getDeclaredAnnotations(),
        () -> resourceInfo.getResourceClass().getCanonicalName() + ":"
            + resourceInfo.getResourceMethod().getName());
    if (annotation == null) {
      annotation = getAnnotation(
          resourceInfo.getResourceMethod().getDeclaringClass().getDeclaredAnnotations(),
          () -> resourceInfo.getResourceClass().getCanonicalName());
    }

    return annotation;
  }

  private boolean hasSecurityAnnotations(Method method) {
    return Stream.of(method.getAnnotations())
        .anyMatch(annotation -> mpJwtAnnotations.contains(annotation.annotationType()));
  }

  private boolean isResourceMethod(Method method) {
    // resource methods are methods annotated with an annotation that is itself annotated with
    // @HttpMethod
    // (see JAX-RS spec, chapter 3.3)
    return Stream.of(method.getAnnotations()).anyMatch(
        annotation -> annotation.annotationType().getAnnotation(HttpMethod.class) != null);
  }

  static class MpResourceInfo implements ResourceInfo {
    final ResourceInfo delegation;
    final Method method;
    final Class<?> clazz;

    MpResourceInfo(ResourceInfo delegation) {
      this.delegation = delegation;
      method = delegation.getResourceMethod();
      clazz = delegation.getResourceClass();
    }

    static MpResourceInfo of(ResourceInfo delegation) {
      return new MpResourceInfo(delegation);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MpResourceInfo other = (MpResourceInfo) obj;
      if (clazz == null) {
        if (other.clazz != null) {
          return false;
        }
      } else if (!clazz.equals(other.clazz)) {
        return false;
      }
      if (method == null) {
        if (other.method != null) {
          return false;
        }
      } else if (!method.equals(other.method)) {
        return false;
      }
      return true;
    }

    public ResourceInfo getDelegation() {
      return delegation;
    }

    @Override
    public Class<?> getResourceClass() {
      return clazz;
    }

    @Override
    public Method getResourceMethod() {
      return method;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (clazz == null ? 0 : clazz.hashCode());
      return prime * result + (method == null ? 0 : method.hashCode());
    }

  }
}
