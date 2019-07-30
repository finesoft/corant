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
package corant.suites.keycloak.spi;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 上午11:34:49
 *
 */
public class KeycloakJMSSender {

  static final Logger logger = Logger.getLogger(EventSelector.class);
  static final String MESSAGE_TYPE = "messageType";
  static final String KEYCLOAK_EVENT = "keycloakEvent";
  static final String JMS_CONN_FACTORY_JNDI_NAME = "java:jboss/exported/jms/RemoteArtemis";
  static final String JMS_DEST_JNDI_NAME = "java:jboss/exported/jms/queue/KeycloakEventQueue";

  private final Scope config;
  private final ObjectMapper objectMapper;
  private final String jmsUser;
  private final String jmsPassword;
  private volatile boolean initialized = false;
  private volatile Destination destination;
  private volatile ConnectionFactory connectionFactory;

  public KeycloakJMSSender(Scope config) {
    this.config = config;
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    jmsUser = config.get("jms-user");
    jmsPassword = config.get("jms-password");
  }

  void send(Object event) {
    if (!initialize() || event == null) {
      return;
    }
    try (JMSContext ctx = connectionFactory.createContext(jmsUser, jmsPassword)) {
      String text = objectMapper.writeValueAsString(event);
      JMSProducer jp = ctx.createProducer();
      TextMessage textMessage = ctx.createTextMessage(text);
      textMessage.setStringProperty(MESSAGE_TYPE, KEYCLOAK_EVENT);
      jp.send(destination, textMessage);
      logger.debugf("Sent keycloak event %s", text);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean initialize() {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          try {
            String cf = config.get("jms-connecionFactory-jndi-name", JMS_CONN_FACTORY_JNDI_NAME);
            String dt = config.get("jms-destination-name", JMS_DEST_JNDI_NAME);
            Context ctx = new InitialContext();
            connectionFactory = (ConnectionFactory) ctx.lookup(cf);
            destination = (Destination) ctx.lookup(dt);
            initialized = true;
            logger.infof("Initialize keycloak event message sender %s : %s", cf, dt);
          } catch (NamingException e) {
            throw new RuntimeException("JMS infrastructure lookup failed: " + e.getMessage(), e);
          }
        }
      }
    }
    return initialized;
  }
}
