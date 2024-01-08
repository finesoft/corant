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
package org.corant.modules.microprofile.restclient;

import java.util.logging.Logger;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * corant-modules-microprofile-restclient
 *
 * @deprecated from resteasy_4_5_3
 *             META-INF/services/org.jboss.resteasy.microprofile.client.header.IncomingHeadersProvider
 * @author bingo 下午1:26:52
 */
@Deprecated
public class MpIncomingHeadersProvider /* implements IncomingHeadersProvider */ {

  static final UnmodifiableMultivaluedMap<String, String> EMPTY_MAP =
      new UnmodifiableMultivaluedMap<>(new MultivaluedHashMap<>());// static?

  transient Logger logger = Logger.getLogger(this.getClass().toString());

  // @Override
  public MultivaluedMap<String, String> getIncomingHeaders() {
    MultivaluedMap<String, String> headers = null;
    HttpRequest request = ResteasyProviderFactory.getInstance().getContextData(HttpRequest.class);
    if (request != null) {
      logger.fine(() -> "Propagates current header information to outgoing request.");
      headers = request.getHttpHeaders().getRequestHeaders();
    }
    return headers == null ? EMPTY_MAP : headers;
  }

}
