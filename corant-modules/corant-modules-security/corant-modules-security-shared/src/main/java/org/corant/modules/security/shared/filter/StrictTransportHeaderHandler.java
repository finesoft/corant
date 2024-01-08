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
package org.corant.modules.security.shared.filter;

import java.util.Optional;
import java.util.function.BiConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-security-shared
 *
 * <p>
 * Some code base from <a href=
 * "https://www.stubbornjava.com/posts/configuring-security-headers-in-undertow">configuring-security-headers-in-undertow,
 * stubbornjava.com.</a>, if there is infringement, please inform me(finesoft@gmail.com).
 *
 * <p>
 * The Strict-Transport-Security header essentially tells the browser that once it has connected to
 * a host over TLS and receives this header, all subsequent requests should be forced to be TLS as
 * well up until the expiration. Since we should all be sending our traffic over HTTPS these days
 * this is a no brainer and very simple to implement if you already support HTTPS everywhere.
 *
 * @see <a href=
 *      "https://scotthelme.co.uk/hsts-the-missing-link-in-tls/">hsts-the-missing-link-in-tls</a>
 * @author bingo 上午11:31:17
 */
@ApplicationScoped
public class StrictTransportHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String STRICT_TRANSPORT_SECURITY_STRING = "Strict-Transport-Security";

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.strict-transport-security.max-age")
  protected Optional<Long> maxAge;

  @Inject
  @ConfigProperty(
      name = "corant.security.filter.header.strict-transport-security.include-sub-domains",
      defaultValue = "false")
  protected boolean includeSubDomains;

  @Override
  public void accept(BiConsumer<String, String> t) {
    maxAge.ifPresent(x -> {
      if (includeSubDomains) {
        t.accept(STRICT_TRANSPORT_SECURITY_STRING, "max-age=" + x + "; includeSubDomains");
      } else {
        t.accept(STRICT_TRANSPORT_SECURITY_STRING, "max-age=" + x);
      }
    });
  }

}
