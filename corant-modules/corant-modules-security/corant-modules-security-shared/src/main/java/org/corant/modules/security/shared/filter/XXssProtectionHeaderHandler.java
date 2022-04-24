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

import java.util.function.BiConsumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-security-shared
 * <p>
 * Some code base from <a href=
 * "https://www.stubbornjava.com/posts/configuring-security-headers-in-undertow">configuring-security-headers-in-undertow,
 * stubbornjava.com.</a>, if there is infringement, please inform me(finesoft@gmail.com).
 *
 * <p>
 * This header will enable some built in browser XSS protection with
 * <a href="https://scotthelme.co.uk/hardening-your-http-response-headers/#x-xss-protection">varying
 * modes.</a>
 *
 * @see <a href=
 *      "https://scotthelme.co.uk/hardening-your-http-response-headers/#x-content-type-options"/>x-content-type-options</a>
 *
 * @author bingo 上午11:47:29
 *
 */
@ApplicationScoped
public class XXssProtectionHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String X_XSS_PROTECTION_STRING = "X-Xss-Protection";

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.x-xss-protection.enable",
      defaultValue = "false")
  protected boolean enable;

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.x-xss-protection.block",
      defaultValue = "false")
  protected boolean block;

  @Override
  public void accept(BiConsumer<String, String> t) {
    if (enable) {
      if (block) {
        t.accept(X_XSS_PROTECTION_STRING, "1; mode=block");
      } else {
        t.accept(X_XSS_PROTECTION_STRING, "1");
      }
    }
  }
}
