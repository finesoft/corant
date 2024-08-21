/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerier;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 15:12:03
 */
public class DefaultJaxrsNamedQuerier extends AbstractNamedQuerier implements JaxrsNamedQuerier {

  protected JaxrsNamedQueryClientConfig clientConfig;
  protected WebTarget target;
  protected JaxrsQueryParameter parameter;

  protected DefaultJaxrsNamedQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryHandler, FetchQueryHandler fetchQueryHandler,
      JaxrsNamedQueryClientConfig clientConfig, WebTarget target, JaxrsQueryParameter parameter) {
    super(query, queryParameter, queryHandler, fetchQueryHandler);
    this.clientConfig = clientConfig;
    this.target = target;
    this.parameter = parameter;
  }

  @Override
  public JaxrsNamedQueryClientConfig getClientConfig() {
    return clientConfig;
  }

  @Override
  public JaxrsQueryParameter getParameter() {
    return parameter;
  }

  @Override
  public WebTarget getTarget() {
    return target;
  }
}
