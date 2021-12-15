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
package org.corant.modules.jaxrs.resteasy;

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.corant.modules.servlet.WebMetaDataProvider;
import org.corant.modules.servlet.metadata.WebInitParamMetaData;
import org.corant.modules.servlet.metadata.WebServletMetaData;
import org.corant.shared.util.Classes;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 下午3:42:37
 *
 */
@ApplicationScoped
public class ResteasyProvider implements WebMetaDataProvider {

  @Inject
  protected ResteasyCdiExtension extension;

  protected final List<WebServletMetaData> servletMetaDatas = new ArrayList<>();

  protected final Map<String, Object> servletContextAttributes = new HashMap<>();

  @Override
  public Map<String, Object> servletContextAttributes() {
    return servletContextAttributes;
  }

  @Override
  public Stream<WebServletMetaData> servletMetaDataStream() {
    return servletMetaDatas.stream();
  }

  @PostConstruct
  protected void onPostConstruct() {
    Instance<Application> applications = select(Application.class);
    if (applications.isResolvable()) {
      ApplicationInfo appInfo = new ApplicationInfo(applications.get());
      servletMetaDatas.add(appInfo.toWebServletMetaData());
      servletContextAttributes.put(ResteasyDeployment.class.getName(),
          appInfo.toResteasyDeployment(d -> {
            d.setScannedResourceClasses(extension.getResources().stream().map(Classes::getUserClass)
                .map(Class::getName).collect(Collectors.toList()));
            d.setScannedProviderClasses(extension.getProviders().stream().map(Classes::getUserClass)
                .map(Class::getName).collect(Collectors.toList()));
          }));
    }
  }

  /**
   * corant-modules-jaxrs-resteasy
   *
   * @author bingo 下午9:06:17
   *
   */
  public static class ApplicationInfo {

    final Application application;
    final Class<?> applicationClass;
    final Class<? extends HttpServletDispatcher> dispatcherClass;
    final String contextPath;
    final int loadOnStartup;

    /**
     * @param application
     */
    public ApplicationInfo(Application application) {
      this.application = application;
      applicationClass = getUserClass(application.getClass());
      ApplicationPath ap = findAnnotation(applicationClass, ApplicationPath.class, true);
      String cp = ap == null ? "/" : ap.value();
      if (isNotEmpty(cp) && cp.charAt(0) != '/') {
        cp = "/" + cp;
      }
      contextPath = cp;
      ResteasyApplication restApp =
          findAnnotation(applicationClass, ResteasyApplication.class, true);
      dispatcherClass = restApp == null ? org.corant.modules.jaxrs.resteasy.ResteasyServlet.class
          : restApp.value();
      loadOnStartup = restApp == null ? 1 : restApp.loadOnStartup();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ApplicationInfo other = (ApplicationInfo) obj;
      if (contextPath == null) {
        if (other.contextPath != null) {
          return false;
        }
      } else if (!contextPath.equals(other.contextPath)) {
        return false;
      }
      return true;
    }

    /**
     *
     * @return the application
     */
    public Application getApplication() {
      return application;
    }

    /**
     *
     * @return the applicationClass
     */
    public Class<?> getApplicationClass() {
      return applicationClass;
    }

    /**
     *
     * @return the contextPath
     */
    public String getContextPath() {
      return contextPath;
    }

    /**
     *
     * @return the dispatcherClass
     */
    public Class<? extends HttpServletDispatcher> getDispatcherClass() {
      return dispatcherClass;
    }

    /**
     *
     * @return the loadOnStartup
     */
    public int getLoadOnStartup() {
      return loadOnStartup;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (contextPath == null ? 0 : contextPath.hashCode());
    }

    public ResteasyDeployment toResteasyDeployment(Consumer<ResteasyDeployment> handler) {
      ResteasyDeployment deploy = new ResteasyDeploymentImpl();
      deploy.setAddCharset(true);
      deploy.setApplication(application);
      deploy.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
      if (handler != null) {
        handler.accept(deploy);
      }
      return deploy;
    }

    public WebServletMetaData toWebServletMetaData() {
      String pattern =
          contextPath.endsWith("/") ? contextPath.concat("*") : contextPath.concat("/*");
      String diapatchName = dispatcherClass.getSimpleName();
      String appName = getUserClass(applicationClass.getClass()).getSimpleName();
      WebInitParamMetaData[] ipmds =
          {new WebInitParamMetaData(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
              contextPath, null)};
      return new WebServletMetaData(diapatchName, new String[] {pattern}, new String[] {pattern},
          loadOnStartup, ipmds, true, null, null, null, diapatchName.concat("-").concat(appName),
          dispatcherClass, null, null);
    }

  }
}
