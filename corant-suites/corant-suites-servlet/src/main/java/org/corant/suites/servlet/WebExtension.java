/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.corant.suites.servlet.metadata.WebFilterMetaData;
import org.corant.suites.servlet.metadata.WebListenerMetaData;
import org.corant.suites.servlet.metadata.WebServletMetaData;

/**
 * corant-suites-webserver-shared
 *
 * @author bingo 下午1:21:00
 *
 */
public class WebExtension implements Extension, WebMetaDataProvider {

  private final List<WebListenerMetaData> listenerMetaDatas = new ArrayList<>();
  private final List<WebServletMetaData> servletMetaDatas = new ArrayList<>();
  private final List<WebFilterMetaData> filterMetaDatas = new ArrayList<>();

  public boolean defaultServlet() {
    return !filterMetaDatas.isEmpty() && servletMetaDatas.isEmpty();
  }

  @Override
  public Stream<WebFilterMetaData> filterMetaDataStream() {
    return filterMetaDatas.stream();
  }

  @Override
  public Stream<WebListenerMetaData> listenerMetaDataStream() {
    return listenerMetaDatas.stream();
  }

  @Override
  public Stream<WebServletMetaData> servletMetaDataStream() {
    return servletMetaDatas.stream();
  }

  void findFilterMetaDatas(
      @Observes @WithAnnotations({WebFilter.class}) ProcessAnnotatedType<? extends Filter> pat) {
    filterMetaDatas.add(new WebFilterMetaData(pat.getAnnotatedType().getAnnotation(WebFilter.class),
        pat.getAnnotatedType().getJavaClass()));
  }

  void findListenerMetaDatas(@Observes @WithAnnotations({
      WebListener.class}) ProcessAnnotatedType<? extends ServletContextListener> pat) {
    listenerMetaDatas.add(new WebListenerMetaData(pat.getAnnotatedType().getJavaClass()));
  }

  void findServletMetaDatas(@Observes @WithAnnotations({
      WebServlet.class}) ProcessAnnotatedType<? extends HttpServlet> pat) {
    ServletSecurity servletSecurity = null;
    MultipartConfig multipartConfig = null;
    if (pat.getAnnotatedType().isAnnotationPresent(ServletSecurity.class)) {
      servletSecurity = pat.getAnnotatedType().getAnnotation(ServletSecurity.class);
    }
    if (pat.getAnnotatedType().isAnnotationPresent(MultipartConfig.class)) {
      multipartConfig = pat.getAnnotatedType().getAnnotation(MultipartConfig.class);
    }
    servletMetaDatas
        .add(new WebServletMetaData(pat.getAnnotatedType().getAnnotation(WebServlet.class),
            servletSecurity, multipartConfig, pat.getAnnotatedType().getJavaClass()));
  }

}
