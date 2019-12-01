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
package org.corant.devops.test.unit.web;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午7:13:27
 *
 */
public class RandomWebServerPortProducer {

  public static final int START = 2 << 12;
  public static final int RANGE = 2 << 9;
  public static final int MAX_FAIL = 2 << 7;

  public static int getServerPort() {
    int tryTimes = 0;
    ServerSocket serverPort = null;
    while (serverPort == null && tryTimes < MAX_FAIL) {
      serverPort = openServerSocket(new Random().nextInt(RANGE) + START);
      tryTimes++;
    }
    try {
      return shouldNotNull(serverPort, "Can not find available port from range [%s - %s]!", START,
          START + RANGE).getLocalPort();
    } finally {
      try {
        if (serverPort != null) {
          serverPort.close();
        }
      } catch (IOException ex) {
        // Noop!
      }
    }
  }

  protected static ServerSocket openServerSocket(int port) {
    try {
      return new ServerSocket(port);
    } catch (IOException ex) {
      return null;
    }
  }
}
