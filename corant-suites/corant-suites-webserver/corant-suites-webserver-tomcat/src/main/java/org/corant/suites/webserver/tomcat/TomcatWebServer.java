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
package org.corant.suites.webserver.tomcat;

import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.ifBlank;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.ContextResourceEnvRef;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.servlet.WebMetaDataProvider;
import org.corant.suites.webserver.shared.AbstractWebServer;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;
import org.jboss.weld.environment.tomcat.TomcatContainer;

/**
 * corant-suites-webserver-tomcat
 *
 * @author bingo 下午7:56:08
 *
 */
@ApplicationScoped
public class TomcatWebServer extends AbstractWebServer {

  @Inject
  Logger logger;

  @Inject
  TomcatWebServerConfig specConfig;

  @Inject
  @Any
  Instance<TomcatWebServerConfigurator> extraConfigurator;

  private Tomcat server;

  @Override
  public void start() {
    try {
      server = resolveServer();
      if (getPreStartHandlers().map(h -> h.onPreStart(this)).reduce(Boolean::logicalAnd)
          .orElse(Boolean.TRUE)) {
        server.start();
        getPostStartedHandlers().forEach(h -> h.onPostStarted(this));
        logger.info(() -> String.format("Tomcat was started, %s", config.getDescription()));
        server.getServer().await();
      } else {
        logger.info(() -> "Tomcat can not start, due to some PreStartHandler interruption!");
      }
    } catch (LifecycleException e) {
      throw new CorantRuntimeException(e, "Unable to launch tomcat");
    }
  }

  @Override
  public void stop() {
    if (server != null) {
      try {
        getPreStopHandlers().forEach(h -> h.onPreStop(this));
        server.stop();
        server.destroy();
        getPostStoppedHandlers().forEach(h -> h.onPostStopped(this));
        logger.info("Tomcat was stopped!");
      } catch (LifecycleException e) {
        throw new CorantRuntimeException(e, "Unable to stop tomcat");
      }
    }
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (Tomcat.class.isAssignableFrom(cls)) {
      return cls.cast(server);
    } else {
      throw new CorantRuntimeException("Tomcat cannot unwrap %s", cls);
    }
  }

  protected Tomcat resolveServer() {
    Tomcat tomcat = new Tomcat();
    File baseDir = defaultObject(specConfig.getBaseDirFile(),
        FileUtils.createTempDir("tomcat", asString(config.getPort())));
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    tomcat.setHostname(config.getHost());
    tomcat.setPort(config.getPort());
    // init http connector
    Connector dfltConnector = tomcat.getConnector();
    dfltConnector.setProperty("maxThreads", asString(config.getWorkThreads()));
    // init https connector
    if (config.isSecured()) {
      resolveServerSsl(tomcat);
    }
    if (!extraConfigurator.isUnsatisfied()) {
      extraConfigurator.stream().forEach(cfgr -> cfgr.configureConnector(dfltConnector));
    }
    resolveServerContext(tomcat.addContext("", new File(".").getAbsolutePath()));
    tomcat.enableNaming();
    return tomcat;
  }

  protected void resolveServerContext(Context ctx) {
    // resolveServerJndiContext(ctx);

    if (!extraConfigurator.isUnsatisfied()) {
      extraConfigurator.stream().forEach(cfgr -> cfgr.configureContext(ctx));
    }
    ServletContext servletContext = ctx.getServletContext();
    servletContext.setAttribute(WeldServletLifecycle.BEAN_MANAGER_ATTRIBUTE_NAME,
        corant.getBeanManager());
    servletContext.setAttribute(org.jboss.weld.Container.CONTEXT_ID_KEY,
        corant.getBeanManager().getId());
    getServletContextAttributes().forEach(servletContext::setAttribute);
    ctx.addParameter(org.jboss.weld.environment.servlet.Container.CONTEXT_PARAM_CONTAINER_CLASS,
        TomcatContainer.class.getName());
    ctx.addApplicationListener(org.jboss.weld.environment.servlet.Listener.class.getName());

    getListenerMetaDatas().map(l -> l.getClazz().getName()).forEach(ctx::addApplicationListener);

    if (WebMetaDataProvider.isNeedDfltServlet(getFilterMetaDatas(), getServletMetaDatas())) {
      String servletName = "Tomcat";
      Wrapper wrapper = Tomcat.addServlet(ctx, servletName, DefaultServlet.class.getName());
      wrapper.setAsyncSupported(true);
      ctx.addServletMappingDecoded("/*", servletName);
    }
    getServletMetaDatas().forEach(sm -> {
      Wrapper wrapper = Tomcat.addServlet(ctx, ifBlank(sm.getName(), sm.getClazz().getName()),
          sm.getClazz().getName());
      wrapper.setLoadOnStartup(sm.getLoadOnStartup());
      sm.getInitParamsAsMap().forEach(wrapper::addInitParameter);
      wrapper.setAsyncSupported(sm.isAsyncSupported());
      Arrays.stream(sm.getUrlPatterns())
          .forEach(u -> ctx.addServletMappingDecoded(u, sm.getName()));
    });
    getFilterMetaDatas().forEach(fm -> {
      FilterDef filterDef = new FilterDef();
      filterDef.setFilterName(fm.getFilterName());
      filterDef.setFilterClass(fm.getClazz().getName());
      filterDef.setAsyncSupported(Boolean.toString(fm.isAsyncSupported()));
      filterDef.setDisplayName(fm.getDisplayName());
      filterDef.setLargeIcon(fm.getLargeIcon());
      filterDef.setSmallIcon(fm.getSmallIcon());
      filterDef.setDescription(fm.getDescription());
      Arrays.stream(fm.getInitParams())
          .forEach(p -> filterDef.addInitParameter(p.getName(), p.getValue()));
      ctx.addFilterDef(filterDef);
      FilterMap mapping = new FilterMap();
      mapping.setFilterName(fm.getFilterName());
      Arrays.stream(fm.getDispatcherTypes()).map(Enum::name).forEach(mapping::setDispatcher);
      Arrays.stream(fm.getUrlPatterns()).forEach(mapping::addURLPattern);
      mapping.setCharset(Charset.forName("utf-8"));
      ctx.addFilterMap(mapping);
    });
  }

  protected void resolveServerJndiContext(Context ctx) {
    ContextResource bmCtxRec = new ContextResource();
    bmCtxRec.setAuth("Container");
    bmCtxRec.setName("BeanManager");
    bmCtxRec.setScope("Sharable");
    bmCtxRec.setType("javax.enterprise.inject.spi.BeanManage");
    bmCtxRec.setProperty("factory", "org.jboss.weld.resources.ManagerObjectFactory");
    bmCtxRec.setLookupName("java:comp/env/BeanManager");
    ctx.getNamingResources().addResource(bmCtxRec);
    ContextResourceEnvRef bmCtxRecEnv = new ContextResourceEnvRef();
    bmCtxRecEnv.setName("BeanManager");
    bmCtxRecEnv.setType("javax.enterprise.inject.spi.BeanManager");
    ctx.getNamingResources().addResourceEnvRef(bmCtxRecEnv);
  }

  protected void resolveServerSsl(Tomcat tomcat) {
    Connector connector = new Connector();
    connector.setScheme("https");
    connector.setPort(config.getSecuredPort().get());
    connector.setProperty("keystoreFile", config.getKeystorePath().get());
    connector.setProperty("keystorePass", config.getKeystorePassword().get());
    connector.setProperty("keystoreType", config.getKeystoreType().get());
    connector.setProperty("clientAuth", "false");
    connector.setProperty("sslProtocol", "TLS");
    connector.setProperty("maxThreads", ObjectUtils.asString(config.getWorkThreads()));
    connector.setProperty("protocol", specConfig.getProtocol());
    connector.setAttribute("SSLEnabled", true);
    connector.setSecure(true);
    tomcat.getService().addConnector(connector);
  }

}
