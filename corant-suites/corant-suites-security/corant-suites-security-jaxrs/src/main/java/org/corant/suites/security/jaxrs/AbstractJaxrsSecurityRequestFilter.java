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
import java.util.Optional;
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
  @ConfigProperty(name = "security.jarxrs.covered-urls")
  Optional<String> coveredUrls;

  @Inject
  @ConfigProperty(name = "security.jarxrs.uncovered-urls")
  Optional<String> uncoveredUrls;

  protected final CompletePathMatcher coveredCompletePathMatcher = new CompletePathMatcher(true);
  protected final WildcardPathMatcher coveredWildcardPathMatcher = new WildcardPathMatcher(true);
  protected final CompletePathMatcher uncoveredCompletePathMatcher = new CompletePathMatcher(true);
  protected final WildcardPathMatcher uncoveredWildcardPathMatcher = new WildcardPathMatcher(true);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (isCoveredUrl(resolvePath(requestContext))) {
      doSecurityFilter(requestContext);
    }
  }

  protected void addUrls(boolean covered, String... urls) {
    if (covered) {
      for (String url : urls) {
        if (isNotBlank(url)) {
          if (WildcardMatcher.hasWildcard(url)) {
            coveredWildcardPathMatcher.addExpresses(url);
          } else {
            coveredCompletePathMatcher.addCompareds(url);
          }
        }
      }
    } else {
      for (String url : urls) {
        if (isNotBlank(url)) {
          if (WildcardMatcher.hasWildcard(url)) {
            uncoveredWildcardPathMatcher.addExpresses(url);
          } else {
            uncoveredCompletePathMatcher.addCompareds(url);
          }
        }
      }
    }
  }

  protected abstract void doSecurityFilter(ContainerRequestContext requestContext)
      throws IOException;

  protected boolean isCoveredUrl(String url) {
    if (uncoveredCompletePathMatcher.match(url)) {
      return false;
    } else if (uncoveredWildcardPathMatcher.match(url)) {
      return false;
    } else {
      return coveredCompletePathMatcher.match(url) || coveredWildcardPathMatcher.match(url);
    }
  }

  protected void removeUrls(String... urls) {
    coveredWildcardPathMatcher.removeExpresses(urls);
    coveredCompletePathMatcher.removeCompareds(urls);
    uncoveredWildcardPathMatcher.removeExpresses(urls);
    uncoveredCompletePathMatcher.removeCompareds(urls);
  }

  protected String resolvePath(ContainerRequestContext requestContext) {
    return requestContext.getUriInfo().getPath();
  }

  @PostConstruct
  void onPostConstruct() {
    coveredUrls.ifPresent(u -> addUrls(true, split(u, ";")));
    uncoveredUrls.ifPresent(u -> addUrls(false, split(u, ";")));
  }
}
