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
package org.corant.modules.microprofile.jwt;

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.ws.rs.core.SecurityContext;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.security.SecurityContextManager;
import org.corant.modules.security.shared.DefaultSecurityContext;
import org.corant.modules.security.shared.IdentifierPrincipal;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午4:49:16
 *
 */
@RequestScoped
public class MpJWTSecurityContextManager implements SecurityContextManager<SecurityContext> {
  static final Logger logger = Logger.getLogger(MpJWTSecurityContextManager.class.getName());

  @Override
  public void bind(SecurityContext securityContext) {
    if (securityContext != null && securityContext.getUserPrincipal() != null) {
      logger.fine(() -> "Bind current microprofile-JWT principal to SecurityContexts.");
      JsonWebToken principal =
          shouldInstanceOf(securityContext.getUserPrincipal(), JsonWebToken.class);
      Map<String, Serializable> map = new HashMap<>();
      for (String cn : principal.getClaimNames()) {
        Object co = principal.getClaim(cn);
        if (!"raw_token".equals(cn)) {
          map.put(cn, convert(co));
        }
      }
      SecurityContexts
          .setCurrent(new DefaultSecurityContext(securityContext.getAuthenticationScheme(),
              new IdentifierPrincipal(principal.getSubject(), principal.getName(), map)));
    } else {
      logger.fine(() -> "Bind empty security context to SecurityContexts.");
      SecurityContexts.setCurrent(null);
    }
  }

  @Override
  public void unbind() {
    logger.fine(() -> "Unbind current security context from SecurityContexts.");
    SecurityContexts.setCurrent(null);
  }

  @PreDestroy
  void onPreDestroy() {
    unbind();
  }

  private Serializable convert(Object claimValue) {
    if (claimValue instanceof JsonString) {
      return ((JsonString) claimValue).getString();
    } else if (claimValue instanceof JsonNumber) {
      JsonNumber jcv = (JsonNumber) claimValue;
      if (jcv.isIntegral()) {
        return Long.valueOf(jcv.longValue());
      } else {
        return Double.valueOf(jcv.doubleValue());
      }
    } else if (claimValue instanceof JsonArray) {
      JsonArray ja = (JsonArray) claimValue;
      ArrayList<Serializable> list = new ArrayList<>(ja.size());
      for (JsonValue jv : ja) {
        list.add(convert(jv));
      }
      return list;
    } else if (claimValue instanceof JsonObject) {
      JsonObject jo = (JsonObject) claimValue;
      HashMap<String, Serializable> map = new HashMap<>(jo.size());
      jo.forEach((k, v) -> {
        map.put(k, convert(v));
      });
      return map;
    } else if (claimValue instanceof Serializable) {
      return (Serializable) claimValue;
    } else {
      return claimValue != null ? claimValue.toString() : null;
    }
  }
}
