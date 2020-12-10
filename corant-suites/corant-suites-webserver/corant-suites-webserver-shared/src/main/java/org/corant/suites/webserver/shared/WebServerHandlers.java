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
package org.corant.suites.webserver.shared;

/**
 * corant-suites-webserver-shared
 *
 * @author bingo 下午3:30:43
 *
 */
public interface WebServerHandlers {

  @FunctionalInterface
  interface PostStartedHandler {

    static int compare(PostStartedHandler h1, PostStartedHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    void onPostStarted(WebServer webServer);
  }

  @FunctionalInterface
  interface PostStoppedHandler {

    static int compare(PostStoppedHandler h1, PostStoppedHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    void onPostStopped(WebServer webServer);
  }

  @FunctionalInterface
  interface PreStartHandler {

    static int compare(PreStartHandler h1, PreStartHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    boolean onPreStart(WebServer webServer);
  }

  @FunctionalInterface
  interface PreStopHandler {

    static int compare(PreStopHandler h1, PreStopHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    boolean onPreStop(WebServer webServer);
  }
}
