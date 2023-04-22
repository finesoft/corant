package org.corant.modules.mail;

import static org.corant.shared.ubiquity.Throwing.uncheckedFunction;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.corant.config.Configs;
import org.corant.shared.resource.Resource;

/**
 * corant-modules-mail
 *
 * @author jiang 2021/2/20
 */
@ApplicationScoped
public class DefaultMailSender implements MailSender {

  @Inject
  Logger logger;

  @Override
  public void send(Function<Session, MimeMessage> messageProvider) throws MessagingException {
    this.send(messageProvider.apply(getSession()));
  }

  public void send(MimeMessage mimeMessage) throws MessagingException {
    MailConfig config = getConfig();
    logger.log(Level.FINE,
        () -> String.format("Connecting to %s:%s", config.getHost(), config.getPort()));
    try (Transport transport = getSession().getTransport(config.getProtocol())) {
      transport.connect(config.getHost(), config.getPort(), config.getUsername(),
          config.getPassword());
      if (mimeMessage.getSentDate() == null) {
        mimeMessage.setSentDate(new Date());
      }
      String messageId = mimeMessage.getMessageID();
      mimeMessage.saveChanges();
      if (messageId != null) {
        mimeMessage.setHeader("Message-ID", messageId);
      }
      logger.log(Level.FINE, () -> String.format("Sending message id : %s using host: %s",
          messageId, config.getHost()));
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
    }
  }

  @Override
  public void send(String subject, String htmlMessage, List<String> toAddressList,
      Resource... resources) throws MessagingException {
    MailConfig config = getConfig();
    Multipart multipart = new MimeMultipart();
    BodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(htmlMessage, "text/html;charset=utf-8");
    multipart.addBodyPart(htmlPart);
    MimeMessage mimeMessage = new MimeMessage(getSession());
    mimeMessage.setFrom(config.getUsername());
    mimeMessage.setContent(multipart);
    mimeMessage.setSubject(subject);
    InternetAddress[] toAddresses = toAddressList.stream()
        .map(uncheckedFunction(InternetAddress::new)).toArray(InternetAddress[]::new);
    mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses);
    this.send(mimeMessage);
  }

  @Override
  public void send(String subject, String htmlMessage, String toAddress, String ccAddress)
      throws MessagingException {
    MailConfig config = getConfig();
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

  protected MailConfig getConfig() {
    return Configs.resolveSingle(MailConfig.class);
  }

  protected Session getSession() {
    MailConfig config = getConfig();
    return Session.getInstance(config.getMailProperties(), config.getAuthenticator());
  }
}
