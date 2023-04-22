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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;

/**
 * corant-modules-microprofile-restclient
 *
 * @author bingo 下午7:14:19
 *
 */
@ApplicationScoped
public class MpClientHeadersFactory implements ClientHeadersFactory {

  protected static final UnmodifiableMultivaluedMap<String, String> EMPTY_MAP =
      new UnmodifiableMultivaluedMap<>(new MultivaluedHashMap<>());// static?

  @Override
  public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    if (incomingHeaders != null) {
      headers.putAll(incomingHeaders);
    }
    if (clientOutgoingHeaders != null) {
      headers.putAll(clientOutgoingHeaders);
    }
    return headers;
  }

}
