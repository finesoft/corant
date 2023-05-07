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
package org.corant.modules.microprofile.jwt.cdi;

import static org.corant.shared.util.Classes.tryAsClass;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.corant.modules.microprofile.jwt.jaxrs.MpJWTAuthJaxRsFeature;
import org.corant.modules.microprofile.jwt.jaxrs.MpJWTAuthenticationFilter;
import org.corant.modules.microprofile.jwt.servlet.MpJWTHttpAuthenticationMechanism;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JWTCallerPrincipalFactoryProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.OptionalClaimTypeProducer;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午6:00:57
 *
 */
public class MpSmallRyeJWTAuthCDIExtension implements Extension {

  public static final String ENABLE_EE_SECURITY = "corant.security.ee.enable";

  private static Logger logger = Logger.getLogger(MpSmallRyeJWTAuthCDIExtension.class);

  public static boolean isHttpAuthMechanismEnabled() {
    return tryAsClass(
        "javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism") != null
        && ConfigProvider.getConfig().getOptionalValue(ENABLE_EE_SECURITY, Boolean.class)
            .orElse(false);
  }

  protected boolean registerOptionalClaimTypeProducer() {
    return false;
  }

  void addAnnotatedType(BeforeBeanDiscovery event, BeanManager beanManager, Class<?> type) {
    final String id = "SmallRye" + type.getSimpleName();
    event.addAnnotatedType(beanManager.createAnnotatedType(type), id);
    logger.debugf("Added type: %s.", type.getName());
  }

  void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
    logger.debugf("beanManager = %s.", beanManager);

    // TODO: Do not add CDI beans unless @LoginConfig (or other trigger) is configured
    addAnnotatedType(event, beanManager, ClaimValueProducer.class);
    addAnnotatedType(event, beanManager, CommonJwtProducer.class);
    addAnnotatedType(event, beanManager, DefaultJWTParser.class);
    addAnnotatedType(event, beanManager, JWTCallerPrincipalFactoryProducer.class);
    addAnnotatedType(event, beanManager, JsonValueProducer.class);
    addAnnotatedType(event, beanManager, JWTAuthContextInfoProvider.class);
    addAnnotatedType(event, beanManager, PrincipalProducer.class);
    addAnnotatedType(event, beanManager, RawClaimTypeProducer.class);
    if (registerOptionalClaimTypeProducer()) {
      addAnnotatedType(event, beanManager, OptionalClaimTypeProducer.class);
    }
    if (isHttpAuthMechanismEnabled()) {
      addAnnotatedType(event, beanManager, MpJWTHttpAuthenticationMechanism.class);
      logger.debugf("EE Security is available, JWTHttpAuthenticationMechanism has been registered");
    } else {
      addAnnotatedType(event, beanManager, MpJWTAuthJaxRsFeature.class);
      addAnnotatedType(event, beanManager, MpJWTAuthenticationFilter.class);
      logger.infof(
          "EE Security is not available, JWTHttpAuthenticationMechanism will not be registered");
    }
  }

}
