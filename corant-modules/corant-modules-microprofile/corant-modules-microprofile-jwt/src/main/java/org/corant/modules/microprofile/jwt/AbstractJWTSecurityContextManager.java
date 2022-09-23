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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.security.Principal;
import org.corant.modules.security.SecurityContextManager;
import org.corant.modules.security.shared.DefaultSecurityContext;
import org.corant.modules.security.shared.IdentifiablePrincipal;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午9:32:18
 *
 */
public abstract class AbstractJWTSecurityContextManager<C> implements SecurityContextManager<C> {

  protected static final Logger logger =
      Logger.getLogger(AbstractJWTSecurityContextManager.class.getName());

  @Override
  public void unbind() {
    logger.fine(() -> "Unbind current security context from SecurityContexts.");
    SecurityContexts.setCurrent(null);
  }

  protected void bindJsonWebToken(JsonWebToken userPrincipal, String authSchema) {
    if (userPrincipal != null) {
      logger.fine(() -> "Bind current microprofile-JWT principal to SecurityContexts.");
      Map<String, Serializable> map = new HashMap<>();
      for (String cn : userPrincipal.getClaimNames()) {
        Object co = userPrincipal.getClaim(cn);
        if (!"raw_token".equals(cn)) {
          map.put(cn, convert(co));
        }
      }

      Serializable id = userPrincipal.getSubject();
      String name = userPrincipal.getName();
      Principal principal = new IdentifiablePrincipal(id, name, map);
      SecurityContexts.setCurrent(new DefaultSecurityContext(authSchema, principal));
    } else {
      logger.fine(() -> "Bind empty security context to SecurityContexts.");
      SecurityContexts.setCurrent(null);
    }
  }

  protected Serializable convert(Object claimValue) {
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
