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
package org.corant.microprofile.jwt.jaxrs;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import org.corant.microprofile.jwt.cdi.MpSmallRyeJWTAuthCDIExtension;
import org.eclipse.microprofile.auth.LoginConfig;
import org.jboss.logging.Logger;
import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;

/**
 * corant-microprofile-jwt
 *
 * @author bingo 下午6:40:31
 *
 */
@Provider
public class MpSmallRyeJWTAuthJaxRsFeature implements Feature {

  private static Logger logger = Logger.getLogger(MpSmallRyeJWTAuthJaxRsFeature.class);

  @Context
  private Application restApplication;

  @Override
  public boolean configure(FeatureContext context) {
    boolean enabled = mpJwtEnabled();
    if (enabled) {
      context.register(MpJWTAuthorizationFilterRegistrar.class);
      context.register(MpBlackListFilter.class);
      if (!MpSmallRyeJWTAuthCDIExtension.isHttpAuthMechanismEnabled()) {
        context.register(MpJWTAuthenticationFilter.class);
        logger.debugf("EE Security is not in use, %s has been registered",
            JWTAuthenticationFilter.class.getSimpleName());
      }
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
}
