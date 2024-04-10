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
package org.corant.modules.servlet;

import static org.corant.shared.util.Configurations.getConfigValue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import org.corant.modules.servlet.metadata.WebFilterMetaData;
import org.corant.modules.servlet.metadata.WebListenerMetaData;
import org.corant.modules.servlet.metadata.WebServletMetaData;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Services;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午1:21:00
 */
public class WebExtension implements Extension, WebMetaDataProvider {

  protected static final boolean cdiDiscovery =
      getConfigValue("corant.servlet.discovery-by-cdi", Boolean.class, Boolean.TRUE);
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

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    listenerMetaDatas.clear();
    servletMetaDatas.clear();
    filterMetaDatas.clear();
  }

  void findFilterMetaDatas(
      @Observes @WithAnnotations({WebFilter.class}) ProcessAnnotatedType<? extends Filter> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    filterMetaDatas.add(new WebFilterMetaData(pat.getAnnotatedType().getAnnotation(WebFilter.class),
        pat.getAnnotatedType().getJavaClass()));
  }

  void findListenerMetaDatas(@Observes @WithAnnotations({
      WebListener.class}) ProcessAnnotatedType<? extends ServletContextListener> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    listenerMetaDatas.add(new WebListenerMetaData(pat.getAnnotatedType().getJavaClass()));
  }

  void findServletMetaDatas(@Observes @WithAnnotations({
      WebServlet.class}) ProcessAnnotatedType<? extends HttpServlet> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
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

  // Jandex
  void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    // TODO use jandex to find out web meta data
    if (!cdiDiscovery) {
      throw new NotSupportedException("For now we only support cdi descovery!");
    }
  }

}
