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
package org.corant.suites.webserver.undertow;

import static org.corant.kernel.normal.Defaults.DFLT_CHARSET_STR;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import org.corant.kernel.normal.Names;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.Resources.SourceType;
import org.corant.shared.util.StopWatch;
import org.corant.suites.servlet.metadata.HttpConstraintMetaData;
import org.corant.suites.servlet.metadata.ServletSecurityMetaData;
import org.corant.suites.servlet.metadata.WebListenerMetaData;
import org.corant.suites.webserver.shared.AbstractWebServer;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;
import org.jboss.weld.environment.undertow.UndertowContainer;
import org.xnio.Options;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.api.SessionPersistenceManager;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

/**
 * corant-suites-webserver-undertow
 *
 * @author bingo 下午3:00:15
 *
 */
@ApplicationScoped
public class UndertowWebServer extends AbstractWebServer {

  @Inject
  Logger logger;

  @Inject
  UndertowWebServerConfig specConfig;

  @Inject
  @Any
  Instance<UndertowWebServerConfigurator> additionalConfigurators;

  @Inject
  @Any
  Instance<SessionPersistenceManager> sessionPersistenceManager;

  private Undertow server;

  @Override
  public void start() {
    try {
      StopWatch sw = StopWatch.press(getUserClass(getClass()).getSimpleName());
      server = resolveServer();
      if (getPreStartHandlers().map(h -> h.onPreStart(this)).reduce(Boolean::logicalAnd)
          .orElse(Boolean.TRUE)) {
        server.start();
        getPostStartedHandlers().forEach(h -> h.onPostStarted(this));
        sw.destroy((t) -> logger.info(() -> String.format("%s [%s] was started, takes %s seconds.",
            t.getLastTaskInfo().getTaskName(), config.getDescription(),
            t.getLastTaskInfo().getTimeSeconds())));
      } else {
        logger.info(() -> "Undertow can not start, due to some PreStartHandler interruption!");
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e, "Unable to launch undertow ");
    }
  }

  @Override
  public void stop() {
    if (server != null) {
      try {
        getPreStopHandlers().forEach(h -> h.onPreStop(this));
        server.stop();
        getPostStoppedHandlers().forEach(h -> h.onPostStopped(this));
      } catch (Exception e) {
        throw new CorantRuntimeException(e, "Unable to stop undertow ");
      }
    }
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (Undertow.class.isAssignableFrom(cls)) {
      return cls.cast(server);
    } else {
      throw new CorantRuntimeException("Undertow can not unwrap %s ", cls);
    }
  }

  protected EmptyRoleSemantic resolveEmptyRoleSemantic(HttpConstraintMetaData hcm) {
    if (hcm != null
        && hcm.getValue() == javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic.PERMIT) {
      return EmptyRoleSemantic.PERMIT;
    } else {
      return EmptyRoleSemantic.DENY;
    }
  }

  protected void resolveFilterInfo(DeploymentInfo di) {
    getFilterMetaDatas().forEach(wfm -> {
      if (wfm != null) {
        FilterInfo fi = new FilterInfo(wfm.getFilterName(), wfm.getClazz());
        fi.setAsyncSupported(wfm.isAsyncSupported());
        wfm.getInitParamsAsMap().forEach(fi::addInitParam);
        di.addFilter(fi);
        streamOf(wfm.getServletNames()).forEach(sn -> {
          streamOf(wfm.getDispatcherTypes()).forEach(dt -> {
            di.addFilterServletNameMapping(wfm.getFilterName(), sn, dt);
          });
        });
        streamOf(wfm.getUrlPatterns()).forEach(url -> {
          streamOf(wfm.getDispatcherTypes()).forEach(dt -> {
            di.addFilterUrlMapping(wfm.getFilterName(), url, dt);
          });
        });
      }
    });
  }

  protected void resolveJsp(DeploymentInfo di) {
    // TODO FIXME
  }

  protected void resolveListener(DeploymentInfo di) {
    // weld listener
    di.addListener(new ListenerInfo(org.jboss.weld.environment.servlet.Listener.class));
    getListenerMetaDatas().map(WebListenerMetaData::getClazz).map(ListenerInfo::new)
        .forEach(di::addListener);
  }

  protected Undertow resolveServer() throws IOException {
    Undertow.Builder builder = Undertow.builder();
    resolveSocketOptions(builder);
    resolveServerOptions(builder);
    resolveWorkerOptions(builder);
    if (specConfig.isEnableAjp()) {
      builder.addAjpListener(config.getPort(), config.getHost());
    }
    builder.addHttpListener(config.getPort(), config.getHost())
        .setBufferSize(specConfig.getBufferSize());
    if (config.isSecured()) {
      builder.addHttpsListener(config.getSecuredPort().get(), config.getHost(),
          resolveSSLContext());
    }
    DeploymentManager deploymentManager = resolveServerDeploymentManager();
    deploymentManager.deploy();
    try {
      deploymentManager.getDeployment().getSessionManager()
          .setDefaultSessionTimeout(config.getSessionTimeout());
      HttpHandler servletHandler = deploymentManager.start();
      PathHandler handler =
          Handlers.path(Handlers.redirect("/")).addPrefixPath("/", servletHandler);
      resolveStaticContent(handler);
      builder.setHandler(handler);
      Undertow server = builder.build();
      return server;
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected DeploymentManager resolveServerDeploymentManager() {
    String name = Names.applicationName() + "-undertow";
    DeploymentInfo di = new DeploymentInfo();
    di.setContextPath(config.getContextPath());
    di.setDeploymentName(name);
    di.setDefaultEncoding(config.getDefaultCharset().orElse(DFLT_CHARSET_STR));
    di.setDisplayName(config.getDisplayName().orElse(name));
    di.setResourceManager(new ClassPathResourceManager(getClass().getClassLoader()));
    di.setClassLoader(getClass().getClassLoader());
    di.setEagerFilterInit(specConfig.isEagerFilterInit());
    if (specConfig.isPersistenceSession() && sessionPersistenceManager.isResolvable()) {
      di.setSessionPersistenceManager(sessionPersistenceManager.get());
    }
    config.getLocaleCharsetMap().forEach(di::addLocaleCharsetMapping);
    di.addServletContextAttribute(WeldServletLifecycle.BEAN_MANAGER_ATTRIBUTE_NAME,
        corant.getBeanManager());
    getServletContextAttributes().forEach(di::addServletContextAttribute);
    di.addInitParameter(org.jboss.weld.environment.servlet.Container.CONTEXT_PARAM_CONTAINER_CLASS,
        UndertowContainer.class.getName());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureDeployment(di));
    }
    // listener
    resolveListener(di);
    // web socket endpoint
    resolveWebSocket(di);
    // servlet
    resolveServletInfo(di);
    // filter
    resolveFilterInfo(di);
    return Servlets.defaultContainer().addDeployment(di);
  }

  protected void resolveServerOptions(Builder builder) {
    builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, specConfig.getNotRequestTimeout());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureServerOptions(builder::setServerOption));
    }
  }

  protected void resolveServletInfo(DeploymentInfo di) {

    if (specConfig.isEnableDefaultServlet()) {
      di.addServlet(Servlets.servlet("default", DefaultServlet.class));
    }

    getServletMetaDatas().map(wsm -> {
      if (wsm != null) {
        ServletInfo si = new ServletInfo(wsm.getName(), wsm.getClazz());
        wsm.getInitParamsAsMap().forEach(si::addInitParam);
        si.addMappings(wsm.getUrlPatterns());
        si.setLoadOnStartup(wsm.getLoadOnStartup());
        si.setAsyncSupported(wsm.isAsyncSupported());
        if (wsm.getSecurity() != null) {
          ServletSecurityInfo ssi = new ServletSecurityInfo();
          ServletSecurityMetaData ssm = wsm.getSecurity();
          if (ssm.getHttpConstraint() != null) {
            ssi.addRolesAllowed(ssm.getHttpConstraint().getRolesAllowed());
            resolveTransportGuaranteeType(ssi, ssm.getHttpConstraint().getTransportGuarantee());
            ssi.setEmptyRoleSemantic(resolveEmptyRoleSemantic(ssm.getHttpConstraint()));
            streamOf(ssm.getHttpMethodConstraints()).map(m -> m.getValue())
                .map(m -> new HttpMethodSecurityInfo().setMethod(m))
                .forEach(ssi::addHttpMethodSecurityInfo);
          }
          si.setServletSecurityInfo(ssi);
        }
        if (wsm.getMultipartConfig() != null) {
          si.setMultipartConfig(new MultipartConfigElement(wsm.getMultipartConfig().getLocation(),
              wsm.getMultipartConfig().getMaxFileSize(),
              wsm.getMultipartConfig().getMaxRequestSize(),
              wsm.getMultipartConfig().getFileSizeThreshold()));
        }
        return si;
      }
      return null;
    }).filter(ObjectUtils::isNotNull).forEach(di::addServlet);
  }

  protected void resolveSocketOptions(Builder builder) {
    builder.setSocketOption(Options.WORKER_IO_THREADS, specConfig.getIoThreads())
        .setSocketOption(Options.TCP_NODELAY, specConfig.isTcpNoDelay())
        .setSocketOption(Options.REUSE_ADDRESSES, specConfig.isReuseAddress())
        .setSocketOption(Options.BALANCING_TOKENS, specConfig.getBalancingTokens())
        .setSocketOption(Options.BALANCING_CONNECTIONS, specConfig.getBalancingConnections())
        .setSocketOption(Options.BACKLOG, specConfig.getBackLog());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureSocketOptions(builder::setSocketOption));
    }
  }

  protected void resolveStaticContent(PathHandler handler) {
    if (specConfig.getStaticContentPath().isPresent()
        && specConfig.getStaticServingPath().isPresent()) {
      String path = specConfig.getStaticContentPath().get();
      String servPath = specConfig.getStaticServingPath().get();
      SourceType st = SourceType.decide(path).orElse(SourceType.CLASS_PATH);
      String usePath = st.resolve(path);
      if (st == SourceType.FILE_SYSTEM) {
        handler.addPrefixPath(servPath,
            new ResourceHandler(new PathResourceManager(Paths.get(usePath))));
      } else {
        // TODO FIXME virtual path handler...
        handler.addPrefixPath(servPath, new ResourceHandler(
            new ClassPathResourceManager(this.getClass().getClassLoader(), usePath)));
      }
    }
  }

  protected void resolveTransportGuaranteeType(ServletSecurityInfo ssi, TransportGuarantee std) {
    if (std == TransportGuarantee.CONFIDENTIAL) {
      ssi.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
    } else {
      ssi.setTransportGuaranteeType(TransportGuaranteeType.NONE);
    }
  }

  protected void resolveWebSocket(DeploymentInfo di) {
    if (!isEmpty(webSocketExtension.getEndpointClasses())) {
      WebSocketDeploymentInfo wsdi = new WebSocketDeploymentInfo();
      webSocketExtension.getEndpointClasses().forEach(wsdi::addEndpoint);
      di.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsdi);
    }
  }

  protected void resolveWorkerOptions(Builder builder) {
    builder.setWorkerOption(Options.WORKER_IO_THREADS, specConfig.getIoThreads())
        .setWorkerOption(Options.CONNECTION_HIGH_WATER, specConfig.getHighWater())
        .setWorkerOption(Options.CONNECTION_LOW_WATER, specConfig.getLowWater())
        .setWorkerOption(Options.WORKER_TASK_CORE_THREADS, config.getWorkThreads())
        .setWorkerOption(Options.WORKER_TASK_MAX_THREADS, config.getWorkThreads())
        .setWorkerOption(Options.TCP_NODELAY, specConfig.isTcpNoDelay())
        .setWorkerOption(Options.CORK, specConfig.isCork());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureWorkOptions(builder::setWorkerOption));
    }
  }

}
