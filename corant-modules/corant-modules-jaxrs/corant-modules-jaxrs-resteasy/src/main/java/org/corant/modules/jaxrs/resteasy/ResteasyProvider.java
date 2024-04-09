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

import static org.corant.context.Beans.find;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.corant.modules.jaxrs.shared.JaxrsExtension;
import org.corant.modules.servlet.WebMetaDataProvider;
import org.corant.modules.servlet.metadata.WebInitParamMetaData;
import org.corant.modules.servlet.metadata.WebServletMetaData;
import org.corant.shared.ubiquity.Configurator;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Services;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 下午3:42:37
 */
@ApplicationScoped
public class ResteasyProvider implements WebMetaDataProvider {

  public static final Application DEFAULT_APPLICATION = new DefaultApplication() {};

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = "corant.resteasy.application.use-default-if-unresolved",
      defaultValue = "true")
  protected Boolean useDefaultApplicationIfUnresolved;

  @Inject
  @ConfigProperty(name = "corant.resteasy.application.alternative-if-unresolved")
  protected Optional<String> alternativeApplicationIfUnresolved;

  @Inject
  protected JaxrsExtension extension;

  protected final List<WebServletMetaData> servletMetaDatas = new ArrayList<>();

  protected final Map<String, Object> servletContextAttributes = new HashMap<>();

  protected volatile ApplicationInfo applicationInfo;

  public ApplicationInfo getApplicationInfo() {
    return applicationInfo;
  }

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

    Application application = find(Application.class).orElseGet(() -> {
      if (alternativeApplicationIfUnresolved.isPresent()) {
        return (Application) Objects.newInstance(asClass(alternativeApplicationIfUnresolved.get()));
      } else if (useDefaultApplicationIfUnresolved) {
        return DEFAULT_APPLICATION;
      } else {
        return null;
      }
    });
    if (application != null) {
      logger.info(() -> String.format("Jaxrs application: %s",
          getUserClass(application).getCanonicalName()));
      applicationInfo = new ApplicationInfo(application);
      servletMetaDatas.add(applicationInfo.toWebServletMetaData());
      servletContextAttributes.put(ResteasyDeployment.class.getName(),
          applicationInfo.toResteasyDeployment(d -> {
            d.setScannedResourceClasses(extension.getResources().stream().map(Classes::getUserClass)
                .map(Class::getName).collect(Collectors.toList()));
            d.setScannedProviderClasses(extension.getProviders().stream().map(Classes::getUserClass)
                .map(Class::getName).collect(Collectors.toList()));
          }));
    } else {
      logger.info(() -> "Jaxrs application not found!");
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
    final String applicationPath;
    final int loadOnStartup;

    public ApplicationInfo(Application application) {
      this.application = application;
      applicationClass = getUserClass(application.getClass());
      ApplicationPath ap = findAnnotation(applicationClass, ApplicationPath.class, true);
      String cp = ap == null ? "/" : ap.value();
      if (isNotEmpty(cp) && cp.charAt(0) != '/') {
        cp = "/" + cp;
      }
      applicationPath = cp;
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
      if (applicationPath == null) {
        if (other.applicationPath != null) {
          return false;
        }
      } else if (!applicationPath.equals(other.applicationPath)) {
        return false;
      }
      return true;
    }

    public Application getApplication() {
      return application;
    }

    public Class<?> getApplicationClass() {
      return applicationClass;
    }

    public String getApplicationPath() {
      return applicationPath;
    }

    public Class<? extends HttpServletDispatcher> getDispatcherClass() {
      return dispatcherClass;
    }

    public int getLoadOnStartup() {
      return loadOnStartup;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (applicationPath == null ? 0 : applicationPath.hashCode());
    }

    public ResteasyDeployment toResteasyDeployment(Consumer<ResteasyDeployment> handler) {
      ResteasyDeployment deploy = new ResteasyDeploymentImpl();
      deploy.setAddCharset(true);
      deploy.setApplication(application);
      deploy.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
      if (handler != null) {
        handler.accept(deploy);
      }
      Services.selectRequired(Configurator.class, this.getClass().getClassLoader())
          .sorted(Sortable::compare).filter(c -> c.supports(deploy)).forEach(c -> c.accept(deploy));
      return deploy;
    }

    public WebServletMetaData toWebServletMetaData() {
      String pattern = applicationPath.endsWith("/") ? applicationPath.concat("*")
          : applicationPath.concat("/*");
      String diapatchName = dispatcherClass.getSimpleName();
      String appName = applicationClass.getSimpleName();
      WebInitParamMetaData[] ipmds =
          {new WebInitParamMetaData(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
              applicationPath, null)};
      return new WebServletMetaData(diapatchName, new String[] {pattern}, new String[] {pattern},
          loadOnStartup, ipmds, true, null, null, null, diapatchName.concat("-").concat(appName),
          dispatcherClass, null, null);
    }

  }

  /**
   * corant-modules-jaxrs-resteasy
   *
   * @author bingo 下午3:14:28
   *
   */
  @ApplicationPath("jaxrs")
  public static class DefaultApplication extends Application {

  }
}
