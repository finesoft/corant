package org.corant.asosat.ddd.service;

import org.corant.Corant;
import org.corant.kernel.spi.CorantBootHandler;

public class Log4jProvider implements CorantBootHandler {

  @Override
  public void handleAfterStarted(Corant corant, String... args) {}

  @Override
  public void handleBeforeStart(ClassLoader classLoader, String... args) {
    System.clearProperty("java.util.logging.manager");
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  }

}
