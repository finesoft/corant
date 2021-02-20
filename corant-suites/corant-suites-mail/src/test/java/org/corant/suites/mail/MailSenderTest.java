package org.corant.suites.mail;

import junit.framework.TestCase;
import org.corant.Corant;
import org.corant.context.Instances;
import org.corant.shared.exception.CorantRuntimeException;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Properties;

/**
 * @author jiang 2021/2/18
 * @since
 */
public class MailSenderTest extends TestCase {

  @Test
  public void test() throws MessagingException {
    Corant.run(
        () -> {
          try {
            MailSender mailSender = Instances.resolve(MailSender.class);
            mailSender.send(
                "my test subject form p360!!!",
                "<h1> This is my html !!!!!!</h1>",
                List.of("35416870@qq.com"));
          } catch (Exception e) {
            throw new CorantRuntimeException(e);
          }
        });
  }

  @Test
  public void testSendMail() throws MessagingException {
    Properties properties = new Properties();
    properties.setProperty("mail.transport.protocol", "smtp"); // 发送邮件协议
    properties.setProperty("mail.smtp.auth", "true"); // 需要验证
    // properties.setProperty("mail.debug", "true");//设置debug模式 后台输出邮件发送的过程
    Session session = Session.getInstance(properties);
    session.setDebug(true); // debug模式
    // 邮件信息
    Multipart multipart = new MimeMultipart();
    BodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent("<h1> This is my html </h1>", "text/html");
    multipart.addBodyPart(htmlPart);

    Message message = new MimeMessage(session);
    message.setFrom(new InternetAddress("enquiry@parts360.cn")); // 设置发送人
    message.setContent(multipart);
    //        message.setText("test text form p360");//设置邮件内容
    message.setSubject("test subject form p360"); // 设置邮件主题
    // 发送邮件
    Transport tran = session.getTransport();
    tran.connect("smtp.exmail.qq.com", 25, "enquiry@parts360.cn", "Gx306813"); // 连接到新浪邮箱服务器
    tran.sendMessage(message, new Address[] {new InternetAddress("35416870@qq.com")}); // 设置邮件接收人
    tran.close();
  }
}
