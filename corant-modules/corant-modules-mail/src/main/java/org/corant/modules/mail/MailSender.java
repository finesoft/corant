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
package org.corant.modules.mail;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;

/**
 * corant-modules-mail
 *
 * @author bingo 下午12:09:38
 */
public interface MailSender {

  void send(List<String> to, String subject, String message, Resource... resources);

  void send(Mail... mail) throws MessagingException;

  void send(String to, String subject, String message, Resource... resources);

  void send(String to, String cc, String subject, String message, Resource... resources);

  /**
   * corant-modules-mail
   *
   * @author bingo 22:47:09
   */
  class DefaultAuthenticator extends Authenticator {

    final PasswordAuthentication token;

    public DefaultAuthenticator(String username, String password) {
      token = new PasswordAuthentication(username, password);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return token;
    }

  }

  /**
   * corant-modules-mail
   *
   * @author bingo 19:50:45
   */
  class DefaultMailSender implements MailSender {

    protected Properties properties;
    protected DefaultAuthenticator authenticator;
    protected String username;
    protected String password;

    public DefaultMailSender(Properties properties) {
      this(properties, properties.getProperty("mail.username"),
          properties.getProperty("mail.password"));
    }

    public DefaultMailSender(Properties properties, String username, String password) {
      this.properties = properties;
      this.username = username;
      this.password = password;
      authenticator = new DefaultAuthenticator(username, password);
    }

    @Override
    public void send(List<String> to, String subject, String text, Resource... resources) {
      this.send(new Mail().from(username).subject(subject).text(text).to(to)
          .attachments(Arrays.asList(resources)));
    }

    @Override
    public void send(Mail... mails) {
      shouldNotNull(mails);
      Session session = getSession();
      try (Transport transport = session.getTransport()) {
        transport.connect();
        for (Mail mail : mails) {
          if (mail.from() == null && username != null) {
            mail.from(username);
          }
          MimeMessage mimeMessage = mail.toMimeMessage(session);
          if (mimeMessage.getSentDate() == null) {
            mimeMessage.setSentDate(new Date());
          }
          transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    @Override
    public void send(String to, String subject, String text, Resource... resources) {
      this.send(new Mail().from(username).subject(subject).text(text).to(to)
          .attachments(Arrays.asList(resources)));
    }

    @Override
    public void send(String to, String cc, String subject, String text, Resource... resources) {
      this.send(new Mail().from(username).subject(subject).text(text).to(to).cc(cc));
    }

    protected Session getSession() {
      return Session.getDefaultInstance(properties, authenticator);
    }
  }
}
