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
package org.corant.modules.servlet;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.servlet.metadata.WebFilterMetaData;
import org.corant.modules.servlet.metadata.WebListenerMetaData;
import org.corant.modules.servlet.metadata.WebServletMetaData;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午5:04:02
 *
 */
public interface WebMetaDataProvider {

  static boolean isNeedDfltServlet(Stream<WebFilterMetaData> filters,
      Stream<WebServletMetaData> servlets) {
    return filters.count() > 0 && servlets.count() == 0;
  }

  default Stream<WebFilterMetaData> filterMetaDataStream() {
    return Stream.empty();
  }

  default Stream<WebListenerMetaData> listenerMetaDataStream() {
    return Stream.empty();
  }

  default Map<String, Object> servletContextAttributes() {
    return Collections.emptyMap();
  }

  default Stream<WebServletMetaData> servletMetaDataStream() {
    return Stream.empty();
  }
}
