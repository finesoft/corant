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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.corant.config.Configs;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-security-shared
 *
 * <p>
 * Some code base from <a href=
 * "https://www.stubbornjava.com/posts/configuring-security-headers-in-undertow">configuring-security-headers-in-undertow,
 * stubbornjava.com.</a>, if there is infringement, please inform me(finesoft@gmail.com).
 *
 * <p>
 * The Content-Security-Policy header is a way to lock down what types of resources are allowed to
 * be loaded from specific sources. This can be very finely controlled or use broader defaults
 * <a href="https://scotthelme.co.uk/content-security-policy-an-introduction/">available CSP
 * options.</a> This header is great to set for early stage projects but can be quite a bit more of
 * a chore for legacy sites. In order to set very strict rules you need to ensure your site doesn't
 * have any inline scripting (including Google Analytics! <a href=
 * "https://github.com/StubbornJava/StubbornJava/pull/63/files#diff-ffe78c1d67e97b7efc7f76fca84476a3">see
 * here</a>) as well as no inline CSS styles (yikes!). Since this header can be quite verbose, make
 * sure it is only set on pages that load HTML documents. This header is essentially useless on
 * things like JSON APIs and can add quite a few bytes per request which could add up for high
 * traffic endpoints.
 *
 * @author bingo 下午12:44:57
 */
@ApplicationScoped
public class ContentSecurityPolicyHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String CONTENT_SECURITY_POLICY_STRING = "Content-Security-Policy";

  protected String headerValues;

  @Override
  public void accept(BiConsumer<String, String> t) {
    if (isNotBlank(headerValues)) {
      t.accept(CONTENT_SECURITY_POLICY_STRING, headerValues);
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    Map<String, ContentSecurityPolicyConfig> policyMap =
        Configs.resolveMulti(ContentSecurityPolicyConfig.class);
    if (isNotEmpty(policyMap)) {
      headerValues = policyMap.entrySet().stream()
          .map(entry -> entry.getKey() + " " + String.join(" ", entry.getValue().getPolicies()))
          .collect(Collectors.joining("; "));
    } else {
      headerValues = null;
    }
  }

  public enum ContentSecurityPolicy {
    NONE("'none'"), // blocks the use of this type of resource.
    SELF("'self'"), // matches the current origin (but not subdomains).
    UNSAFE_INLINE("'unsafe-inline'"), // allows the use of inline JS and CSS.
    UNSAFE_EVAL("'unsafe-eval'"); // allows the use of mechanisms like eval().

    private final String value;

    ContentSecurityPolicy(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  @ConfigKeyRoot(value = "corant.security.filter.header.content-security-policy")
  public static class ContentSecurityPolicyConfig extends AbstractNamedObject
      implements DeclarativeConfig {

    private static final long serialVersionUID = 8210352333492813455L;

    @ConfigKeyItem
    private Set<String> policies;

    public Set<String> getPolicies() {
      return policies;
    }

    @Override
    public boolean isValid() {
      return isNotEmpty(policies);
    }

    @Override
    public void onPostConstruct(Config config, String key) {
      setName(key);
    }
  }

}
