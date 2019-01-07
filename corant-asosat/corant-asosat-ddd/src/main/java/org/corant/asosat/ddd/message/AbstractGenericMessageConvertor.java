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
package org.corant.asosat.ddd.message;

import static org.corant.kernel.util.Preconditions.requireNotEmpty;
import static org.corant.kernel.util.Preconditions.requireNull;
import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassPaths;
import org.corant.shared.util.ClassPaths.ClassInfo;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;
import org.corant.suites.ddd.message.MessageService.MessageConvertor;
import org.corant.suites.jpa.shared.JpaUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author bingo 下午3:28:14
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractGenericMessageConvertor<P, A> implements MessageConvertor {

  public static final String MSG_QUE_SPT = ";";

  @Inject
  @Any
  @ConfigProperty(name = "app.message.class.packages", defaultValue = "com.;cn.")
  protected String localMsgClsPath;

  @Inject
  @Any
  @ConfigProperty(name = "app.packages", defaultValue = "com.;cn.")
  protected String msgClsPath;

  protected final Map<String, Constructor<AbstractGenericMessage<P, A>>> constructors =
      new HashMap<>();

  public AbstractGenericMessageConvertor() {}

  @Override
  public AbstractGenericMessage<P, A> from(ExchangedMessage message) {
    if (message != null) {
      Constructor<AbstractGenericMessage<P, A>> ctr = this.constructors.get(message.queueName());
      if (ctr != null) {
        try {
          return ctr.newInstance(message);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
          throw new GeneralRuntimeException(e, PkgMsgCds.ERR_EXMSG_CVT, message.queueName());
        }
      }
    }
    return null;
  }

  @PreDestroy
  protected void destroy() {}

  @PostConstruct
  @SuppressWarnings("unchecked")
  protected synchronized void enable() {
    Set<String> pkgs = asSet(split(this.localMsgClsPath, MSG_QUE_SPT));
    pkgs.addAll(asSet(split(this.msgClsPath, MSG_QUE_SPT)));
    for (String path : pkgs) {
      if (isNotBlank(path)) {
        try {
          ClassPaths.from(path).getClasses().map(ClassInfo::load).forEach(cls -> {
            if (cls != null && AbstractGenericMessage.class.isAssignableFrom(cls)
                && JpaUtils.isPersistenceClass(cls)) {// only support JPA
              Class<AbstractGenericMessage<P, A>> msgCls =
                  (Class<AbstractGenericMessage<P, A>>) cls;
              Constructor<AbstractGenericMessage<P, A>> match = this.findConstructor(msgCls);
              if (match != null) {
                requireNotEmpty(MessageUtils.extractMessageQueues(cls),
                    PkgMsgCds.ERR_MSG_CFG_QUEUE_NULL, cls.getName()).forEach(queue -> {
                      requireNull(this.constructors.put(queue, match),
                          PkgMsgCds.ERR_MSG_CFG_QUEUE_DUP, queue);
                    });
              }
            }
          });
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        }
      }
    }
  }

  protected Constructor<AbstractGenericMessage<P, A>> findConstructor(
      Class<AbstractGenericMessage<P, A>> cls) {
    return ConstructorUtils.getMatchingAccessibleConstructor(cls, ExchangedMessage.class);
  }

}
