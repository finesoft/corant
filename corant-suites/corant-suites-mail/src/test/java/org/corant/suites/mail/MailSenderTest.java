package org.corant.suites.mail;

import junit.framework.TestCase;
import org.corant.Corant;
import org.corant.context.Instances;
import org.corant.shared.exception.CorantRuntimeException;
import org.junit.Test;

import javax.mail.MessagingException;
import java.util.List;

/**
 * corant-suites-mail
 *
 * @author jiang 2021/2/20
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
}
