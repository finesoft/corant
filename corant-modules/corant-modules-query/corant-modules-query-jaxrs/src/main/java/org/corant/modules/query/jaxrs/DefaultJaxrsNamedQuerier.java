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

  protected WebTarget target;
  protected WebTargetConfig targetConfig;
  protected JaxrsNamedQueryClientConfig clientConfig;

  protected DefaultJaxrsNamedQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryHandler, FetchQueryHandler fetchQueryHandler,
      JaxrsNamedQueryClientConfig clientConfig, WebTargetConfig targetConfig, WebTarget target) {
    super(query, queryParameter, queryHandler, fetchQueryHandler);
    this.clientConfig = clientConfig;
    this.targetConfig = targetConfig;
    this.target = target;
  }

  @Override
  public JaxrsNamedQueryClientConfig getClientConfig() {
    return clientConfig;
  }

  @Override
  public WebTarget getTarget() {
    return target;
  }

  @Override
  public WebTargetConfig getTargetConfig() {
    return targetConfig;
  }
}
