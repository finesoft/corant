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
package org.corant.suites.mp.jwt;

import static org.corant.shared.util.CollectionUtils.setOf;
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
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.auth.LoginConfig;
import org.jboss.logging.Logger;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 上午10:47:48
 *
 */
@Provider
public class MpJaxRsFeature implements Feature {

  private static Logger logger = Logger.getLogger(MpJaxRsFeature.class);

  private static final Set<Class<? extends Annotation>> mpJwtAnnotations =
      setOf(DenyAll.class, PermitAll.class, RolesAllowed.class);
  private static final MpDenyAllFilter denyAllFilter = new MpDenyAllFilter();
  private static final MpPermitAllFilter permitAllFilter = new MpPermitAllFilter();
  private static final MpJWTAuthenticationFilter jwtFilter = new MpJWTAuthenticationFilter();
  private static final MpBlackListFilter blackListFilter = new MpBlackListFilter();
  private static final MpDynamicFeature dynamicFeature = new MpDynamicFeature();
  @Context
  private Application restApplication;

  @Override
  public boolean configure(FeatureContext context) {
    boolean enabled = mpJwtEnabled();
    if (enabled) {
      context.register(dynamicFeature);
      context.register(jwtFilter);
      context.register(blackListFilter);
      logger.debugf("MP-JWT LoginConfig present, %s is enabled", getClass().getSimpleName());
    } else {
      logger.infof("LoginConfig not found on Application class, %s will not be enabled",
          getClass().getSimpleName());
    }
    return enabled;
  }

  boolean mpJwtEnabled() {
    boolean enabled = false;
    if (restApplication != null) {
      Class<?> applicationClass = restApplication.getClass();
      if (applicationClass.isAnnotationPresent(LoginConfig.class)) {
        LoginConfig config = applicationClass.getAnnotation(LoginConfig.class);
        enabled = "MP-JWT".equals(config.authMethod());
      }
    }
    return enabled;
  }

  public static class MpDynamicFeature implements DynamicFeature {
    private static final Map<ResourceInfo, Consumer<FeatureContext>> handlers =
        new ConcurrentHashMap<>();

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
      handlers.computeIfAbsent(MpResourceInfo.of(resourceInfo), this::buildHandler).accept(context);
    }

    Consumer<FeatureContext> buildHandler(ResourceInfo resourceInfo) {
      return new Consumer<FeatureContext>() {
        private final boolean hasSecurityAnnotations =
            hasSecurityAnnotations(resourceInfo) && shouldNonannotatedMethodsBeDenied();

        @Override
        public void accept(FeatureContext featureContext) {
          Annotation mpJwtAnnotation = getMpJwtAnnotation(resourceInfo);
          if (mpJwtAnnotation != null) {
            if (mpJwtAnnotation instanceof DenyAll) {
              configureDenyAll(featureContext);
            } else if (mpJwtAnnotation instanceof RolesAllowed) {
              configureRolesAllowed((RolesAllowed) mpJwtAnnotation, featureContext);
            } else if (mpJwtAnnotation instanceof PermitAll) {
              configurePermitAll(featureContext);
            }
          } else {
            if (hasSecurityAnnotations) {
              configureDenyAll(featureContext);
            }
          }
        }
      };
    }

    boolean hasSecurityAnnotations(ResourceInfo resource) {
      // resource methods are inherited (see JAX-RS spec, chapter 3.6)
      // resource methods must be `public` (see JAX-RS spec, chapter 3.3.1)
      // hence `resourceClass.getMethods` -- returns public methods, including inherited ones
      return Stream.of(resource.getResourceClass().getMethods()).filter(this::isResourceMethod)
          .anyMatch(this::hasSecurityAnnotations);
    }

    boolean shouldNonannotatedMethodsBeDenied() {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      URL resource = loader.getResource("/META-INF/MP-JWT-DENY-NONANNOTATED-METHODS");
      return resource != null;
    }

    private void configureDenyAll(FeatureContext context) {
      context.register(denyAllFilter);
    }

    private void configurePermitAll(FeatureContext context) {
      context.register(permitAllFilter);
    }

    private void configureRolesAllowed(RolesAllowed mpJwtAnnotation, FeatureContext context) {
      context.register(new MpRolesAllowedFilter(mpJwtAnnotation.value()));
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
          throw new RuntimeException("Duplicate MicroProfile JWT annotations found on "
              + annotationPlacementDescriptor.get() + ". Expected at most 1 annotation, found: "
              + annotations);
      }
    }

    private Annotation getMpJwtAnnotation(ResourceInfo resourceInfo) {
      Annotation annotation =
          getAnnotation(resourceInfo.getResourceMethod().getDeclaredAnnotations(),
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
      result = prime * result + (method == null ? 0 : method.hashCode());
      return result;
    }

  }
}
