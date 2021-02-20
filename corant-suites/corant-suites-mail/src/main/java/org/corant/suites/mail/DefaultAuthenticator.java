package org.corant.suites.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class DefaultAuthenticator extends Authenticator {

  private final String username;
  private final String password;

  public DefaultAuthenticator(String username, String password) {
    this.password = password;
    this.username = username;
  }

  /** Returns an authenticator object for use in sessions */
  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(username, password);
  }
}
