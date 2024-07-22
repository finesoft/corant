/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mail;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.mapOf;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;
import jakarta.mail.MessagingException;
import org.corant.modules.mail.MailSender.DefaultMailSender;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;

/**
 * corant-modules-mail
 *
 * @author bingo 14:58:00
 */
public class MailSenderTemplate {

  protected String username;

  protected String password;

  protected String protocol = "smtp";

  protected Properties properties = new Properties();

  public MailSenderTemplate() {}

  public MailSenderTemplate auth(boolean auth) {
    properties.put("mail." + protocol + ".auth", Boolean.toString(auth));
    return this;
  }

  public MailSenderTemplate host(String host) {
    properties.put("mail." + protocol + ".host", shouldNotBlank(host));
    return this;
  }

  public MailSenderTemplate password(String password) {
    this.password = password;
    return this;
  }

  public MailSenderTemplate port(int port) {
    properties.put("mail." + protocol + ".port", Integer.toString(port));
    return this;
  }

  public MailSenderTemplate properties(Properties properties) {
    this.properties.clear();
    if (properties != null) {
      this.properties.putAll(properties);
    }
    return this;
  }

  public MailSenderTemplate protocol(String protocol) {
    this.protocol = shouldNotBlank(protocol);
    return this;
  }

  public MailSenderTemplate putProperties(Object... kvs) {
    Map<String, String> newPros = mapOf(kvs);
    properties.putAll(newPros);
    return this;
  }

  public MailSenderTemplate removePropertiesIf(Predicate<String> predicate) {
    List<String> ks =
        properties.keySet().stream().map(Objects::toString).filter(predicate).toList();
    for (String k : ks) {
      properties.remove(k);
    }
    return this;
  }

  public void send(Mail... mails) {
    MailSender sender = resolveSender();
    for (Mail mail : mails) {
      if (mail.from() == null && username != null) {
        mail.from(username);
      }
      try {
        sender.send(mail);
      } catch (MessagingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  public void send(String to, String subject, String message, Resource... resources) {
    MailSender sender = resolveSender();
    sender.send(to, subject, message, resources);
  }

  public void send(String to, String cc, String subject, String message, Resource... resources) {
    MailSender sender = resolveSender();
    sender.send(to, cc, subject, message, resources);
  }

  public MailSenderTemplate socketFactory(Class<?> clazz) {
    properties.put("mail." + protocol + ".socketFactory.class",
        shouldNotNull(clazz).getCanonicalName());
    return this;
  }

  public MailSenderTemplate socketFactoryFallback(boolean fallback) {
    properties.put("mail." + protocol + ".socketFactory.fallback", Boolean.toString(fallback));
    return this;
  }

  public MailSenderTemplate socketFactoryPort(int port) {
    properties.put("mail." + protocol + ".socketFactory.port", Integer.toString(port));
    return this;
  }

  public MailSenderTemplate username(String username) {
    this.username = username;
    return this;
  }

  protected MailSender resolveSender() {
    return new DefaultMailSender(properties, username, password);
  }
}
