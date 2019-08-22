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
package org.corant.suites.jms.shared;

import static java.util.Collections.newSetFromMap;
import static org.corant.Corant.instance;
import static org.corant.kernel.util.Instances.resolveNamed;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.jms.ConnectionFactory;
import org.corant.kernel.util.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.annotation.MessageStream;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:53:23
 *
 */
public abstract class AbstractJMSExtension implements Extension {

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final Set<AnnotatedMethod<?>> receiveMethods = newSetFromMap(new ConcurrentHashMap<>());
  protected final Set<AnnotatedMethod<?>> streamMethods = newSetFromMap(new ConcurrentHashMap<>());
  protected volatile NamedQualifierObjectManager<? extends AbstractJMSConfig> configManager =
      NamedQualifierObjectManager.empty();

  public static AbstractJMSConfig getConfig(String connectionFactoryId) {
    if (instance().select(AbstractJMSExtension.class).isResolvable()) {
      return instance().select(AbstractJMSExtension.class).get().getConfigManager()
          .get(connectionFactoryId);
    }
    return null;
  }

  public static ConnectionFactory getConnectionFactory(String connectionFactoryId) {
    return resolveNamed(ConnectionFactory.class, connectionFactoryId)
        .orElseThrow(() -> new CorantRuntimeException("Can not find connection factory for %s",
            connectionFactoryId));
  }

  public NamedQualifierObjectManager<? extends AbstractJMSConfig> getConfigManager() {
    return configManager;
  }

  public Set<AnnotatedMethod<?>> getReceiveMethods() {
    return Collections.unmodifiableSet(receiveMethods);
  }

  public Set<AnnotatedMethod<?>> getStreamMethods() {
    return Collections.unmodifiableSet(streamMethods);
  }

  protected void onProcessAnnotatedType(@Observes @WithAnnotations({MessageReceive.class,
      MessageStream.class}) ProcessAnnotatedType<?> pat) {
    logger.info(() -> String.format("Scanning message consumer type: %s",
        pat.getAnnotatedType().getJavaClass().getName()));
    final AnnotatedType<?> at = pat.getAnnotatedType();
    for (AnnotatedMethod<?> am : at.getMethods()) {
      if (am.isAnnotationPresent(MessageReceive.class)) {
        logger.info(() -> String.format(
            "Found annotated message consumer method %s %s, adding for further processing.",
            at.getJavaClass().getName(), am.getJavaMember().getName()));
        receiveMethods.add(am);
      } else if (am.isAnnotationPresent(MessageStream.class)) {
        logger.info(() -> String.format(
            "Found annotated message stream method %s %s, adding for further processing,",
            at.getJavaClass().getName(), am.getJavaMember().getName()));
        streamMethods.add(am);
      }
    }
  }

}
