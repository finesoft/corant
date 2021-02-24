package org.corant.suites.mail;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Map;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.toProperties;

/**
 * @author jiang 2021/2/23
 * @since
 */
public class MailSenderTemplate {

  private MailConfig config;

  private MailSenderTemplate(MailConfig config) {
    this.config = shouldNotNull(config);
  }

  public static MailSenderTemplate config(
      String protocol,
      String host,
      int port,
      String username,
      String password,
      int connectionTimeout,
      Map<String, String> properties) {
    MailConfig config =
        new MailConfig(protocol, host, port, username, password, connectionTimeout, properties);
    return new MailSenderTemplate(config);
  }

  public void send(String subject, String htmlMessage, String toAddress, String ccAddress)
      throws MessagingException {
    Multipart multipart = new MimeMultipart();
    BodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(htmlMessage, "text/html;charset=utf-8");
    multipart.addBodyPart(htmlPart);
    MimeMessage mimeMessage = new MimeMessage(getSession());
    mimeMessage.setFrom(config.getUsername());
    mimeMessage.setContent(multipart);
    mimeMessage.setSubject(subject);
    mimeMessage.setRecipients(Message.RecipientType.TO, toAddress);
    if (ccAddress != null) {
      mimeMessage.setRecipients(Message.RecipientType.CC, ccAddress);
    }
    this.send(mimeMessage);
  }

  private void send(MimeMessage mimeMessage) throws MessagingException {
    try (Transport transport = getSession().getTransport(config.getProtocol())) {
      transport.connect(
          config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
      if (mimeMessage.getSentDate() == null) {
        mimeMessage.setSentDate(new Date());
      }
      mimeMessage.saveChanges();
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      System.out.println(String.format("Message is sent using host: %s", config.getHost()));
    }
  }

  private Session getSession() {
    return Session.getInstance(
        toProperties(config.getProperties()),
        new DefaultAuthenticator(config.getUsername(), config.getPassword()));
  }
}
