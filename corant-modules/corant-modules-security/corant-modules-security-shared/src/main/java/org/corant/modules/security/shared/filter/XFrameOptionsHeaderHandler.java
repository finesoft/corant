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
 * The X-Frame-Options allows you to tell the browser which hosts are allowed to load your site in
 * an iframe. This can be important because an attacker could purchase a domain similar to your own,
 * load your entire site in an iframe, then use CSS / JS to add overlaid forms or intercept certain
 * events
 * (<a href="https://scotthelme.co.uk/hardening-your-http-response-headers/#x-frame-options">more
 * details</a>). If you do not rely on iframes it is probably safe to set this to always deny which
 * will prevent other sites from loading your site in an iframe. The ALLOW-FROM option only supports
 * a single host, if you need it to support multiple hosts you will need a dynamic solution along
 * the lines of checking the referrer against a whitelist and dynamically allowing it.
 *
 * @see <a href=
 *      "https://scotthelme.co.uk/hardening-your-http-response-headers/#x-content-type-options"/>x-content-type-options</a>
 *
 * @author bingo 上午11:47:29
 */
@ApplicationScoped
public class XFrameOptionsHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String X_FRAME_OPTIONS_STRING = "X-Frame-Options";

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.x-frame-options.type")
  protected Optional<XFrameOptionType> type;

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.x-frame-options.origin")
  protected Optional<String> origin;

  @Override
  public void accept(BiConsumer<String, String> t) {
    if (type.isPresent()) {
      XFrameOptionType x = type.get();
      switch (x) {
        case DENY:
        case SAMEORIGIN:
          t.accept(X_FRAME_OPTIONS_STRING, x.getValue());
          break;
        default:
          origin.ifPresent(o -> t.accept(X_FRAME_OPTIONS_STRING, x.getValue() + " " + o));
          break;
      }
    }
  }

  public enum XFrameOptionType {
    DENY("DENY"), SAMEORIGIN("SAMEORIGIN"), ALLOW_FROM("ALLOW-FROM ");

    private final String value;

    XFrameOptionType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
