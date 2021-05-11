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
package org.corant.context.security;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.config.Configs;
import org.corant.context.security.Principal.DefaultPrincipal;
import org.corant.context.security.SecurityContext.DefaultSecurityContext;
import org.corant.shared.util.Serializations;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-context
 *
 * @author bingo 下午3:29:59
 *
 */
public interface SecurityContextSerializer {

  SecurityContext deserialize(String securityContext);

  String serialize(SecurityContext securityContext);

  class Base64SecurityContextSerializer implements SecurityContextSerializer {

    public static final Base64SecurityContextSerializer INSTANCE =
        new Base64SecurityContextSerializer();

    static final Logger logger = Logger.getLogger(Base64SecurityContextSerializer.class.getName());

    // FIXME
    static final String serialPrincipalPropertyKeyStr = Configs
        .getValue("corant.context.security.serialization.principal-property-keys", String.class);

    static Set<Predicate<String>> serialPrincipalPropertyKeys = new LinkedHashSet<>();
    static {
      if (isNotBlank(serialPrincipalPropertyKeyStr)) {
        for (String k : split(serialPrincipalPropertyKeyStr, ",", true, true)) {
          if (WildcardMatcher.hasWildcard(k)) {
            serialPrincipalPropertyKeys.add(WildcardMatcher.of(false, k));
          } else {
            serialPrincipalPropertyKeys.add(t -> k.equals(t));
          }
        }
      }
    }

    private Base64SecurityContextSerializer() {}

    @Override
    public SecurityContext deserialize(String securityContext) {
      if (isNotBlank(securityContext)) {
        try {
          byte[] bytes = Base64.getDecoder().decode(securityContext);
          return (SecurityContext) Serializations.deserialize(bytes);
        } catch (Exception e) {
          logger.log(Level.SEVERE, e, () -> "Can't deserialize security context!");
        }
      }
      return null;
    }

    @Override
    public String serialize(SecurityContext securityContext) {
      if (securityContext != null) {
        try {
          SecurityContext ctx = securityContext;
          if (isNotEmpty(serialPrincipalPropertyKeys) && ctx.getPrincipal() != null
              && ctx.getPrincipal().getProperties() != null) {
            Map<String, Serializable> properties =
                new HashMap<>(ctx.getPrincipal().getProperties().size());
            ctx.getPrincipal().getProperties().forEach((k, v) -> {
              if (serialPrincipalPropertyKeys.stream().anyMatch(p -> p.test(k))) {
                properties.put(k, v);
              }
            });
            ctx = new DefaultSecurityContext(ctx.getAuthenticationScheme(), ctx.getSubject(),
                new DefaultPrincipal(ctx.getPrincipal().getName(), properties));
          }
          byte[] bytes = Serializations.serialize(ctx);
          return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
          logger.log(Level.SEVERE, e, () -> "Can't serialize security context!");
        }
      }
      return null;
    }

  }
}
