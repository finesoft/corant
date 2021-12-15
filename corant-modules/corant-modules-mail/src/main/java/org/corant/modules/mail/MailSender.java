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

import java.util.List;
import java.util.function.Function;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.corant.shared.resource.Resource;

/**
 * corant-modules-mail
 *
 * @author bingo 下午12:09:38
 */
public interface MailSender {

  void send(Function<Session, MimeMessage> messageProvider) throws MessagingException;

  void send(String subject, String message, List<String> toAddressList, Resource... resources)
      throws MessagingException;

  void send(String subject, String htmlMessage, String toAddress, String ccAddress)
      throws MessagingException;
}
