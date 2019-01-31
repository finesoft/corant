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
package org.corant.asosat.exp.interfaces;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.sql.DataSource;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午6:39:19
 *
 */
@ApplicationScoped
@WebFilter(filterName = "testFilter", urlPatterns = "/*",
    initParams = {@WebInitParam(name = "param0", value = "value0"),
        @WebInitParam(name = "param1", value = "value1"),
        @WebInitParam(name = "param2", value = "value2")})
public class TestFilter implements Filter {

  FilterConfig config;

  @Inject
  @Named("dmmsRwDs")
  DataSource ds;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // try {
    // new QueryRunner(ds).query("SELECT * FROM EP_Test", new
    // MapListHandler()).stream()
    // .map(m -> "filter inject test ->" + getMapString(m,
    // "name")).forEach(System.out::println);
    // asStream(config.getInitParameterNames()).forEach(p -> {
    // System.out.println("filter init param test ->" + config.getInitParameter(p));
    // });
    // } catch (SQLException e) {
    // throw new CorantRuntimeException(e);
    // }
    System.out.println("hi~~~~~~~~~~~~~~~~~~~~~~~`:)");
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    config = filterConfig;
  }

}
