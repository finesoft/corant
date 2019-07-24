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
package org.corant.asosat.ddd.security;

import static org.corant.kernel.util.Instances.resolve;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.shared.util.ConversionUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:34:06
 *
 */
@ApplicationScoped
public class DefaultSecurityContextProducer implements SecurityContextProducer {

  @Inject
  WeldManager wm;

  @Override
  public DefaultSecurityContext get() {
    if (wm.getActiveContexts().stream().map(c -> c.getScope())
        .anyMatch(c -> c.equals(RequestScoped.class))) {
      Optional<JsonWebToken> jwto = resolve(JsonWebToken.class);
      if (jwto.isPresent()) {
        JsonWebToken jwt = jwto.get();
        Participator currentUser = Participator.empty();
        Participator currentOrg = Participator.empty();
        if (jwt.containsClaim("userId")) {
          String userId = ConversionUtils.toString(jwt.getClaim("userId"));
          String userName = ConversionUtils.toString(jwt.getClaim("preferred_username"));
          currentUser = new Participator(userId, userName);
        }
        if (jwt.containsClaim("orgId")) {
          String orgId = ConversionUtils.toString(jwt.getClaim("orgId"));
          String orgName = ConversionUtils.toString(jwt.getClaim("orgName"));
          currentOrg = new Participator(orgId, orgName);
        }
        return new DefaultSecurityContext(jwt.getRawToken(), null, jwt, currentUser, currentOrg,
            true, "MP-JWT", jwt.getGroups());
      }
    }
    return DefaultSecurityContext.EMPTY_INST;
  }

}
