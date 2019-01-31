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
package org.corant.suites.security.shared;

import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import org.corant.shared.util.PathUtils.GlobPatterns;
import org.corant.suites.security.shared.PathMatcher.CompletePathMatcher;
import org.corant.suites.security.shared.PathMatcher.GlobPathMatcher;
import org.corant.suites.security.shared.PathMatcher.RegexPathMatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-security-jaxrs
 *
 * @author bingo 下午5:08:09
 *
 */
@ApplicationScoped
public abstract class AbstractSecurityRequestFilter {

  @Inject
  @ConfigProperty(name = "security.jarxrs.covered-urls")
  Optional<String> coveredUrls;

  @Inject
  @ConfigProperty(name = "security.jarxrs.uncovered-urls")
  Optional<String> uncoveredUrls;

  protected final CompletePathMatcher coveredCompletePathMatcher = new CompletePathMatcher(true);
  protected final GlobPathMatcher coveredGlobPathMatcher = new GlobPathMatcher(true);
  protected final RegexPathMatcher coveredRegexPathMatcher = new RegexPathMatcher(true);
  protected final CompletePathMatcher uncoveredCompletePathMatcher = new CompletePathMatcher(true);
  protected final GlobPathMatcher uncoveredGlobPathMatcher = new GlobPathMatcher(true);
  protected final RegexPathMatcher uncoveredRegexPathMatcher = new RegexPathMatcher(true);

  protected void addUrl(boolean covered, String url) {
    if (isBlank(url)) {
      return;
    }
    boolean glob = false;
    boolean regex = false;
    int len = url.length();
    for (int i = 0; i < len; i++) {
      if (GlobPatterns.isGlobChar(url.charAt(i))) {
        glob = true;
        continue;
      } else if (GlobPatterns.isRegexChar(url.charAt(i))) {
        regex = true;
        continue;
      }
    }
    if (regex) {
      if (covered) {
        coveredRegexPathMatcher.addExpresses(url);
      } else {
        uncoveredRegexPathMatcher.addExpresses(url);
      }
    } else if (glob) {
      if (covered) {
        coveredGlobPathMatcher.addExpresses(url);
      } else {
        uncoveredGlobPathMatcher.addExpresses(url);
      }
    } else {
      if (covered) {
        coveredCompletePathMatcher.addCompareds(url);
      } else {
        uncoveredCompletePathMatcher.addCompareds(url);
      }
    }
  }

  protected void addUrls(boolean covered, String... urls) {
    for (String url : urls) {
      addUrl(covered, url);
    }
  }

  protected abstract void doSecurityFilter(ContainerRequestContext requestContext)
      throws IOException;

  protected boolean isCoveredUrl(String url) {
    if (uncoveredCompletePathMatcher.match(url)) {
      return false;
    } else if (uncoveredGlobPathMatcher.match(url)) {
      return false;
    } else if (uncoveredRegexPathMatcher.match(url)) {
      return false;
    } else {
      return coveredCompletePathMatcher.match(url) || coveredGlobPathMatcher.match(url)
          || coveredRegexPathMatcher.match(url);
    }
  }

  protected void removeUrls(String... urls) {
    coveredGlobPathMatcher.removeExpresses(urls);
    coveredRegexPathMatcher.removeExpresses(urls);
    coveredCompletePathMatcher.removeCompareds(urls);
    uncoveredGlobPathMatcher.removeExpresses(urls);
    uncoveredCompletePathMatcher.removeCompareds(urls);
    uncoveredRegexPathMatcher.removeExpresses(urls);
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
