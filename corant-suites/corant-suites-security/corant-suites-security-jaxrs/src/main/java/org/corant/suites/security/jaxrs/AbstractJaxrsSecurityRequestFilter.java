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
package org.corant.suites.security.jaxrs;

import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import org.corant.shared.util.StringUtils.WildcardMatcher;
import org.corant.suites.security.jaxrs.PathMatcher.CompletePathMatcher;
import org.corant.suites.security.jaxrs.PathMatcher.WildcardPathMatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-security-jaxrs
 *
 * @author bingo 下午5:08:09
 *
 */
@ApplicationScoped
public abstract class AbstractJaxrsSecurityRequestFilter implements ContainerRequestFilter {

  @Inject
  @ConfigProperty(name = "security.jarxrs.secured-urls")
  Optional<String> configSecuredUrls;

  protected Set<String> securedUrls = new HashSet<>();
  protected Set<String> securedWcUrls = new HashSet<>();

  protected final CompletePathMatcher completePathMatcher = new CompletePathMatcher(true);
  protected final WildcardPathMatcher wildcardPathMatcher = new WildcardPathMatcher(true);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (isSecuredUrl(resolvePath(requestContext))) {
      doSecurityFilter(requestContext);
    }
  }

  protected void addSecuredUrls(String... urls) {
    for (String url : urls) {
      if (isNotBlank(url)) {
        if (WildcardMatcher.hasWildcard(url)) {
          wildcardPathMatcher.addExpresses(url);
        } else {
          completePathMatcher.addCompareds(url);
        }
      }
    }
  }

  protected abstract void doSecurityFilter(ContainerRequestContext requestContext)
      throws IOException;

  protected boolean isSecuredUrl(String url) {
    if (WildcardMatcher.hasWildcard(url)) {
      return wildcardPathMatcher.match(url);
    } else {
      return completePathMatcher.match(url);
    }
  }

  protected void removeSecuredUrls(String... urls) {
    wildcardPathMatcher.removeExpresses(urls);
    completePathMatcher.removeCompareds(urls);
  }

  protected String resolvePath(ContainerRequestContext requestContext) {
    return requestContext.getUriInfo().getPath();
  }

  @PostConstruct
  void onPostConstruct() {
    configSecuredUrls.ifPresent(u -> addSecuredUrls(split(u, ";")));
  }
}
