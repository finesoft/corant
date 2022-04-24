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
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-security-shared
 *
 * <p>
 * Some code base from <a href=
 * "https://www.stubbornjava.com/posts/configuring-security-headers-in-undertow">configuring-security-headers-in-undertow,
 * stubbornjava.com.</a>, if there is infringement, please inform me(finesoft@gmail.com).
 * <p>
 * On the surface it doesn't seem like a referring URL has much to do with security but it could
 * potentially leak some internal information. It's important to set the Referrer-Policy on internal
 * sites. For example as a tech-blog occasionally we see referrer's from applications like JIRA,
 * GITHUB, or even internal tools like Amazons' internal code review tool. Most of these URLs are
 * internal only and cannot be accessed externally from the network. However, especially with vanity
 * URLs they may give away the company that is linking to us, a project name, and sometimes
 * specifics about a particular bug. Couple this with a landing page and you can get more info than
 * you might expect. A real example allowed us to identify a company (which could be figured out by
 * the URL) had a bug related to enum parsing since the JIRA task mentioned it was a bug in the URL
 * and the landing page was Java Enum Lookup by Name or Field Without Throwing Exceptions. For the
 * most part who really cares, but you could be leaking more information than you intend. What if
 * you are linking from internal admin pages now attackers have some information to go off of. Find
 * out specifics about each policy here. If you are a public facing site who drives traffic to
 * external sites there are a few options to choose from that allow you to keep some information if
 * you choose.
 *
 * @see <a href=
 *      "https://scotthelme.co.uk/a-new-security-header-referrer-policy"/>a-new-security-header-referrer-policy</a>
 *
 * @author bingo 上午11:47:29
 *
 */
@ApplicationScoped
public class ReferrerPolicyHeaderHandler implements SecuredHeaderFilterHandler {

  public static final String REFERRER_POLICY_STRING = "Referrer-Policy";

  @Inject
  @ConfigProperty(name = "corant.security.filter.header.referrer-policy")
  protected Optional<ReferrerPolicy> policy;

  @Override
  public void accept(BiConsumer<String, String> t) {
    policy.ifPresent(x -> t.accept(REFERRER_POLICY_STRING, x.value));
  }

  public enum ReferrerPolicy {
    EMPTY(""),

    NO_REFERRER("no-referrer"),

    NO_REFERRER_WHEN_DOWNGRADE("no-referrer-when-downgrade"),

    SAME_ORIGIN("same-origin"),

    ORIGIN("origin"),

    STRICT_ORIGIN("strict-origin"),

    ORIGIN_WHEN_CROSS_ORIGIN("origin-when-cross-origin"),

    STRICT_ORIGIN_WHEN_CROSS_ORIGIN("strict-origin-when-cross-origin"),

    UNSAFE_URL("unsafe-url");

    private final String value;

    ReferrerPolicy(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
