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
package org.corant.suites.mp.restclient;

import static org.corant.kernel.util.Instances.resolve;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.microprofile.client.header.IncomingHeadersProvider;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-suites-mp-restclient
 *
 * @author bingo 下午1:26:52
 *
 */
public class MpIncomingHeadersProvider implements IncomingHeadersProvider {

  public static final String HEADER_AUTHORIZATION_NAME = "Authorization";
  public static final String HEADER_AUTHORIZATION_VALUE = "Bearer ";

  @Override
  public MultivaluedMap<String, String> getIncomingHeaders() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    resolve(WeldManager.class).ifPresent(wm -> {
      if (wm.getActiveContexts().stream().map(c -> c.getScope())
          .anyMatch(c -> c.equals(RequestScoped.class))) {
        resolve(JsonWebToken.class).ifPresent(jwto -> {
          if (isNotBlank(jwto.getRawToken())) {
            headers.put(HttpHeaderNames.AUTHORIZATION,
                listOf(HEADER_AUTHORIZATION_VALUE.concat(jwto.getRawToken())));
          }
        });
      }
    });
    return headers;
  }

}
