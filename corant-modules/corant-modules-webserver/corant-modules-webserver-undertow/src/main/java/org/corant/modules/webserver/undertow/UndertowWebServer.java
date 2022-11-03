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
package org.corant.modules.webserver.undertow;

import static org.corant.shared.normal.Defaults.DFLT_CHARSET_STR;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.split;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import org.corant.modules.servlet.metadata.HttpConstraintMetaData;
import org.corant.modules.servlet.metadata.HttpMethodConstraintMetaData;
import org.corant.modules.servlet.metadata.ServletSecurityMetaData;
import org.corant.modules.servlet.metadata.WebListenerMetaData;
import org.corant.modules.webserver.shared.AbstractWebServer;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Resources;
import org.corant.shared.util.StopWatch;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;
import org.jboss.weld.environment.undertow.UndertowContainer;
import org.xnio.Options;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.api.SessionPersistenceManager;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

/**
 * corant-modules-webserver-undertow
 *
 * @author bingo 下午3:00:15
 *
 */
@ApplicationScoped
public class UndertowWebServer extends AbstractWebServer {

  public static final String HANDLERS_CONF = "META-INF/undertow-handlers.conf";

  @Inject
  protected Logger logger;

  @Inject
  protected UndertowWebServerConfig specConfig;

  @Inject
  @Any
  protected Instance<UndertowWebServerConfigurator> additionalConfigurators;

  @Inject
  @Any
  protected Instance<SessionPersistenceManager> sessionPersistenceManager;

  @Inject
  @Any
  protected Instance<SessionManagerFactory> sessionManagerFactory;

  protected Undertow server;

  protected DeploymentManager deploymentManager;

  @Override
  public void start() {
    try {
      StopWatch sw = StopWatch.press(getUserClass(getClass()).getSimpleName());
      server = resolveServer();
      if (getPreStartHandlers().map(h -> h.onPreStart(this)).reduce(Boolean::logicalAnd)
          .orElse(Boolean.TRUE)) {
        server.start();
        getPostStartedHandlers().forEach(h -> h.onPostStarted(this));
        sw.destroy(t -> logger.info(
            () -> String.format("%s [%s] was started, takes %ss.", t.getLastTaskInfo().getName(),
                config.getDescription(), t.getLastTaskInfo().getTimeSeconds())));
      } else {
        logger.info(() -> "Undertow can not start, due to some PreStartHandler interruption!");
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e, "Unable to launch undertow [%s]",
          config.getDescription());
    }
  }

  @Override
  public void stop() {
    if (server != null) {
      try {
        getPreStopHandlers().forEach(h -> h.onPreStop(this));
        if (deploymentManager != null) {
          deploymentManager.stop();
          deploymentManager.undeploy();
        }
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
      throw new CorantRuntimeException("Undertow server can not unwrap %s ", cls);
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

  protected void resolveFilter(DeploymentInfo di) {
    getFilterMetaDatas().forEach(wfm -> {
      if (wfm != null) {
        FilterInfo fi = new FilterInfo(wfm.getFilterName(), wfm.getClazz());
        fi.setAsyncSupported(wfm.isAsyncSupported());
        wfm.getInitParamsAsMap().forEach(fi::addInitParam);
        di.addFilter(fi);
        logger.fine(() -> String.format("Resolve filter [%s] url patterns [%s].",
            wfm.getClazz().getName(), String.join(", ", wfm.getUrlPatterns())));
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
    getListenerMetaDatas().map(WebListenerMetaData::getClazz).map(ListenerInfo::new).forEach(l -> {
      logger.fine(() -> String.format("Resolve listener [%s].", l.getListenerClass().getName()));
      di.addListener(l);
    });
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
    deploymentManager = resolveServerDeploymentManager();
    deploymentManager.deploy();
    try {
      deploymentManager.getDeployment().getSessionManager()
          .setDefaultSessionTimeout(config.getSessionTimeout());
      HttpHandler handler = deploymentManager.start();
      Optional<ClassPathResource> predicateHandlersConfig =
          Resources.fromClassPath(this.getClass().getClassLoader(), HANDLERS_CONF).findAny();
      if (predicateHandlersConfig.isPresent()) {
        List<PredicatedHandler> predicateHandlers = PredicatedHandlersParser.parse(
            new String(predicateHandlersConfig.get().getBytes(), StandardCharsets.UTF_8),
            this.getClass().getClassLoader());
        if (isNotEmpty(predicateHandlers)) {
          handler = Handlers.predicates(predicateHandlers, handler);
        }
      }
      builder.setHandler(handler);
      return builder.build();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected DeploymentManager resolveServerDeploymentManager() {
    String name = Names.applicationName() + "-undertow";
    DeploymentInfo di = new DeploymentInfo();
    di.setContextPath(config.getContextPath());
    di.setDeploymentName(name);
    di.setDefaultRequestEncoding(specConfig.getDefaultRequestCharset().orElse(DFLT_CHARSET_STR));
    di.setDefaultResponseEncoding(specConfig.getDefaultResponseCharset().orElse(DFLT_CHARSET_STR));
    di.setDefaultEncoding(config.getDefaultCharset().orElse(DFLT_CHARSET_STR));
    di.setDisplayName(config.getDisplayName().orElse(name));
    di.setClassLoader(getClass().getClassLoader());
    di.setEagerFilterInit(specConfig.isEagerFilterInit());
    di.setPreservePathOnForward(false);
    di.addWelcomePages(specConfig.getWelcomePages());
    di.setEscapeErrorMessage(specConfig.isEscapeErrorMessage());// careful
    if (specConfig.isPersistenceSession() && sessionPersistenceManager.isResolvable()) {
      di.setSessionPersistenceManager(sessionPersistenceManager.get());
    }
    if (sessionManagerFactory.isResolvable()) {
      di.setSessionManagerFactory(sessionManagerFactory.get());
    }
    specConfig.getDefaultSessionTimeout().ifPresent(di::setDefaultSessionTimeout);
    config.getLocaleCharsetMap().forEach(di::addLocaleCharsetMapping);
    di.addServletContextAttribute(WeldServletLifecycle.BEAN_MANAGER_ATTRIBUTE_NAME,
        CDI.current().getBeanManager());
    getServletContextAttributes().forEach(di::addServletContextAttribute);
    di.addInitParameter(org.jboss.weld.environment.servlet.Container.CONTEXT_PARAM_CONTAINER_CLASS,
        UndertowContainer.class.getName());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureDeployment(di));
    }
    // static content
    resolveStaticContent(di);
    // listener
    resolveListener(di);
    // web socket endpoint
    resolveWebSocket(di);
    // servlet
    resolveServlet(di);
    // filter
    resolveFilter(di);
    return Servlets.defaultContainer().addDeployment(di);
  }

  protected void resolveServerOptions(Builder builder) {
    if (specConfig.isEnableHttp2()) {
      builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
    }
    builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, specConfig.getNotRequestTimeout());
    if (!additionalConfigurators.isUnsatisfied()) {
      additionalConfigurators.stream().sorted()
          .forEachOrdered(cfgr -> cfgr.configureServerOptions(builder::setServerOption));
    }
  }

  protected void resolveServlet(DeploymentInfo di) {

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
            streamOf(ssm.getHttpMethodConstraints()).map(HttpMethodConstraintMetaData::getValue)
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
        logger.fine(() -> String.format("Resolve servlet [%s], url patterns [%s].", wsm.getName(),
            String.join(", ", wsm.getUrlPatterns())));
        return si;
      }
      return null;
    }).filter(Objects::isNotNull).forEach(di::addServlet);
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

  protected void resolveStaticContent(DeploymentInfo di) {
    List<ResourceManager> managers = new ArrayList<>();
    managers.add(new ClassPathResourceManager(getClass().getClassLoader()));
    Map<String, String> paths = new HashMap<>();
    if (specConfig.getStaticContentPath().isPresent()
        && specConfig.getStaticServingPath().isPresent()) {
      paths.put(specConfig.getStaticServingPath().get(), specConfig.getStaticContentPath().get());
    }
    specConfig.getStaticPaths().ifPresent(ps -> {
      for (String path : split(ps, ";", true, true)) {
        String[] urls = split(path, "->", true, true);
        shouldBeTrue(urls.length == 2);
        paths.put(urls[0], urls[1]);
      }
    });
    paths.forEach((s, c) -> {
      SourceType st = SourceType.decide(c).orElse(SourceType.CLASS_PATH);
      String contentPath = st.resolve(c);
      String servingPath = s;
      logger.fine(() -> String.format("Resolve static content path [%s] -> serving path [%s].",
          contentPath, servingPath));
      if (st == SourceType.FILE_SYSTEM) {
        managers.add(new PathResourceManager(Paths.get(contentPath)));
      } else {
        managers.add(new UndertowClassPathResourceManager(this.getClass().getClassLoader(),
            servingPath, contentPath));
      }
    });
    di.setResourceManager(new UndertowMixedResourceManager(managers));
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
      webSocketExtension.getEndpointClasses().forEach(es -> {
        wsdi.addEndpoint(es);
        logger.fine(() -> String.format("Resolve websocket endpoint class [%s].", es.getName()));
      });
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
