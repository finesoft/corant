/*
 * Copyright (c) 2013-2018. Bingo.Chen (finesoft@gmail.com)
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
 * @author bingo 上午11:37:59
 *
 */
public interface WebServerConfigurator extends Comparable<WebServerConfigurator> {

  @Override
  default int compareTo(WebServerConfigurator o) {
    return Integer.compare(getOrdinal(), o.getOrdinal());
  }

  default int getOrdinal() {
    return 0;
  }
}
