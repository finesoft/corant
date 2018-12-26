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

import java.io.File;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-webserver-tomcat
 *
 * @author bingo 下午7:01:56
 *
 */
@ApplicationScoped
public class TomcatWebServerConfig {

  public final static String CONTEXT_XML =
      "<Context> <Resource name=\"BeanManager\" auth=\"Container\" "
          + "type=\"javax.enterprise.inject.spi.BeanManager\""
          + " factory=\"org.jboss.weld.resources.ManagerObjectFactory\"/></Context>";

  public final static String RESOURCE_ENV_REF =
      "<resource-env-ref> <resource-env-ref-name>BeanManager</resource-env-ref-name> "
          + "<resource-env-ref-type> javax.enterprise.inject.spi.BeanManager </resource-env-ref-type>"
          + "</resource-env-ref>";

  @Inject
  @ConfigProperty(name = "webserver.tomcat.connectionTimeout")
  private Optional<Integer> connectionTimeout;

  @Inject
  @ConfigProperty(name = "webserver.tomcat.maxKeepAliveRequests")
  private Optional<Integer> maxKeepAliveRequests;

  @Inject
  @ConfigProperty(name = "webserver.tomcat.keepAliveTimeout")
  private Optional<Integer> keepAliveTimeout;

  @Inject
  @ConfigProperty(name = "webserver.tomcat.redirectPort")
  private Optional<Integer> redirectPort;

  @Inject
  @ConfigProperty(name = "webserver.tomcat.catalina.base")
  private Optional<String> baseDir;

  @Inject
  @ConfigProperty(name = "webserver.tomcat.connector.protocol",
      defaultValue = "org.apache.coyote.http11.Http11NioProtocol")
  private String protocol;

  /**
   *
   * @return the basedir
   */
  public Optional<String> getBaseDir() {
    return baseDir;
  }

  public File getBaseDirFile() {
    if (baseDir.isPresent()) {
      return new File(baseDir.get());
    }
    return null;
  }

  /**
   *
   * @return the connectionTimeout
   */
  public Optional<Integer> getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   *
   * @return the keepAliveTimeout
   */
  public Optional<Integer> getKeepAliveTimeout() {
    return keepAliveTimeout;
  }

  /**
   *
   * @return the maxKeepAliveRequests
   */
  public Optional<Integer> getMaxKeepAliveRequests() {
    return maxKeepAliveRequests;
  }

  public String getProtocol() {
    return protocol;
  }

  /**
   *
   * @return the redirectPort
   */
  public Optional<Integer> getRedirectPort() {
    return redirectPort;
  }


}
