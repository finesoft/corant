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
package org.corant.suites.jaxrs.resteasy;

import static org.corant.suites.cdi.Instances.select;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.corant.shared.util.AnnotationUtils;
import org.corant.shared.util.ClassUtils;
import org.corant.suites.servlet.WebMetaDataProvider;
import org.corant.suites.servlet.metadata.WebInitParamMetaData;
import org.corant.suites.servlet.metadata.WebServletMetaData;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * corant-suites-jaxrs-resteasy
 *
 * @author bingo 下午3:42:37
 *
 */
@ApplicationScoped
public class ResteasyProvider implements WebMetaDataProvider {

  @Inject
  ResteasyCdiExtension extension;

  final List<WebServletMetaData> servletMetaDatas = new ArrayList<>();

  final Map<String, Object> servletContextAttributes = new HashMap<>();

  @Override
  public Map<String, Object> servletContextAttributes() {
    return servletContextAttributes;
  }

  @Override
  public Stream<WebServletMetaData> servletMetaDataStream() {
    return servletMetaDatas.stream();
  }

  @PostConstruct
  void onPostConstruct() {
    Instance<Application> applications = select(Application.class);
    if (applications.isResolvable()) {
      applications.stream().forEach(this::handle);
      servletContextAttributes.put(ResteasyContextParameters.RESTEASY_ROLE_BASED_SECURITY, true);
    }
  }

  private void handle(Application app) {
    ApplicationPath ap =
        AnnotationUtils.findAnnotation(app.getClass(), ApplicationPath.class, true);
    String contextPath = ap == null ? "/" : ap.value();
    if (!contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    ResteasyDeployment deployment = new ResteasyDeploymentImpl();
    deployment.setAddCharset(true);
    deployment.setApplication(app);
    deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
    deployment.setScannedResourceClasses(
        extension.getResources().stream().map(e -> e.getName()).collect(Collectors.toList()));
    deployment.setScannedProviderClasses(
        extension.getProviders().stream().map(e -> e.getName()).collect(Collectors.toList()));
    handle(app, deployment, contextPath);
  }

  private void handle(Application app, ResteasyDeployment deployment, String contextPath) {
    String pattern = contextPath.endsWith("/") ? contextPath.concat("*") : contextPath.concat("/*");
    WebInitParamMetaData[] ipmds = new WebInitParamMetaData[] {new WebInitParamMetaData(
        ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, contextPath, null)};
    servletMetaDatas.add(new WebServletMetaData("ResteasyServlet", new String[] {pattern},
        new String[] {pattern}, 1, ipmds, true, null, null, null,
        "jaxrs-" + ClassUtils.getShortClassName(app.getClass().getName()),
        HttpServlet30Dispatcher.class, null, null));
    servletContextAttributes.put(ResteasyDeployment.class.getName(), deployment);
  }
}
