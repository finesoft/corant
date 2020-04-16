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
package org.corant.suites.servlet.abstraction;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import org.corant.shared.normal.Defaults;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-servlet
 *
 * @author bingo 下午10:05:46
 *
 */
@ApplicationScoped
@WebFilter(filterName = "CharacterEncodingFilter", urlPatterns = {"/*"})
public class CharacterEncodingFilter implements Filter {

  @Inject
  @ConfigProperty(name = "servlet.request-charset-filter.enable", defaultValue = "true")
  protected boolean enableReq;

  @Inject
  @ConfigProperty(name = "servlet.response-charset-filter.enable", defaultValue = "true")
  protected boolean enableRes;

  @Inject
  @ConfigProperty(name = "servlet.request-charset-filter.charset",
      defaultValue = Defaults.DFLT_CHARSET_STR)
  protected String reqCharset;

  @Inject
  @ConfigProperty(name = "servlet.response-charset-filter.charset",
      defaultValue = Defaults.DFLT_CHARSET_STR)
  protected String resCharset;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // setting the charset
    if (enableReq) {
      request.setCharacterEncoding(reqCharset);
    }
    if (enableRes) {
      response.setCharacterEncoding(resCharset);
    }
    chain.doFilter(request, response);
  }

}
