/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
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
  public interface PostStartedHandler extends Comparable<PostStartedHandler> {
    @Override
    default int compareTo(PostStartedHandler o) {
      return Integer.compare(getOrdinal(), o.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    void onPostStarted(WebServer webServer);
  }

  @FunctionalInterface
  public interface PostStoppedHandler extends Comparable<PostStoppedHandler> {
    @Override
    default int compareTo(PostStoppedHandler o) {
      return Integer.compare(getOrdinal(), o.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    void onPostStopped(WebServer webServer);
  }

  @FunctionalInterface
  public interface PreStartHandler extends Comparable<PreStartHandler> {
    @Override
    default int compareTo(PreStartHandler o) {
      return Integer.compare(getOrdinal(), o.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    boolean onPreStart(WebServer webServer);
  }

  @FunctionalInterface
  public interface PreStopHandler extends Comparable<PreStopHandler> {
    @Override
    default int compareTo(PreStopHandler o) {
      return Integer.compare(getOrdinal(), o.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    boolean onPreStop(WebServer webServer);
  }
}
