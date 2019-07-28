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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 上午11:34:49
 *
 */
@ApplicationScoped
public class KeycloakJMSSender {

  public static final String MESSAGE_TYPE = "messageType";
  public static final String KEYCLOAK_EVENT = "keycloakEvent";

  @Inject
  @JMSConnectionFactory("java:/jms/remoteArtemis")
  JMSContext context;
  Destination destination;
  ObjectMapper objectMapper = new ObjectMapper();

  public void send(Object event) throws Exception {
    if (context == null || destination == null || event == null) {
      return;
    }
    JMSProducer jmsProducer = context.createProducer();
    String text = objectMapper.writeValueAsString(event);
    jmsProducer.send(destination, text);
  }

  @PostConstruct
  void onPostConstruct() {
    destination = context.createTopic("keycloakEvent");
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @PreDestroy
  void onPreDestroy() {
    if (context != null) {
      context.close();
    }
  }
}
