package org.corant.suites.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * corant-suites-mail
 *
 * @author jiang 2021/2/20
 */
public class DefaultAuthenticator extends Authenticator {

  private final String username;
  private final String password;

  public DefaultAuthenticator(String username, String password) {
    this.password = password;
    this.username = username;
  }

  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(username, password);
  }
}