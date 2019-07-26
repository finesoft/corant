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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.corant.shared.exception.CorantRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 上午11:34:49
 *
 */
public class KeycloakJMSSender {
  private static final String MESSAGE_TYPE = "messageType";
  private static final String KEYCLOAK_EVENT = "keycloakEvent";
  private static final String JMS_CONNECTION_FACTORY_JNDI_NAME = "java:/jms/ConnectionFactory";
  private static final String EVENT_DESTINATION_JNDI_NAME = "java:/jms/topic/KeyCloakEvent";
  private final Destination destination;
  private final ConnectionFactory connectionFactory;
  private final ObjectMapper objectMapper;

  public KeycloakJMSSender() {
    try {
      objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      Context ctx = new InitialContext();
      destination = (Destination) ctx.lookup(EVENT_DESTINATION_JNDI_NAME);
      connectionFactory = (ConnectionFactory) ctx.lookup(JMS_CONNECTION_FACTORY_JNDI_NAME);
    } catch (NamingException e) {
      throw new CorantRuntimeException("JMS infrastructure lookup failed: " + e.getMessage(), e);
    }
  }

  void send(Object event) throws Exception {
    if (connectionFactory == null || destination == null || event == null) {
      return;
    }
    try (Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
        MessageProducer messageProducer = session.createProducer(destination)) {
      String text = objectMapper.writeValueAsString(event);
      TextMessage textMessage = session.createTextMessage(text);
      textMessage.setStringProperty(MESSAGE_TYPE, KEYCLOAK_EVENT);
      messageProducer.send(textMessage);
    }
  }
}
