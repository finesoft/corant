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
 * This header basically tells the browser not to try and guess the content-type of a file and only
 * trust the value the server sends.
 *
 * @see <a href=
 *      "https://scotthelme.co.uk/hardening-your-http-response-headers/#x-content-type-options"/>x-content-type-options</a>
 *
 * @author bingo 上午11:47:29
 *
 */
@ApplicationScoped
public class XContentTypeOptionsHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String X_CONTENT_TYPE_OPTIONS_STRING = "X-Content-Type-Options";

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.x-content-type-options.nosniff",
      defaultValue = "false")
  protected boolean nosniff;

  @Override
  public void accept(BiConsumer<String, String> t) {
    if (nosniff) {
      t.accept(X_CONTENT_TYPE_OPTIONS_STRING, "nosniff");
    }
  }

}
