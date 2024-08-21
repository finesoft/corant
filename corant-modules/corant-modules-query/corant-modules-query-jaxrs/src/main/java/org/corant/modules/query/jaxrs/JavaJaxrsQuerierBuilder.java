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

import static org.corant.context.Beans.resolve;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier.JaxrsQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;
import org.corant.shared.ubiquity.Experimental;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:55:32
 */
@Experimental
public class JavaJaxrsQuerierBuilder extends AbstractNamedQuerierBuilder<JaxrsNamedQuerier> {

  protected final Client client;
  protected final JaxrsNamedQueryClientConfig clientConfig;

  protected JavaJaxrsQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler, Client client,
      JaxrsNamedQueryClientConfig clientConfig) {
    super(query, queryHandler, fetchQueryHandler);
    this.client = client;
    this.clientConfig = clientConfig;
  }

  @Override
  public DefaultJaxrsNamedQuerier build(QueryParameter queryParameter) {
    WebTarget target = client.target(clientConfig.getRoot());
    JaxrsQueryParameter parameter =
        resolve(JaxrsQueryParameterResolver.class, NamedLiteral.of(getQuery().getVersionedName()))
            .apply(queryParameter);
    parameter.postConstruct();
    return new DefaultJaxrsNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), clientConfig, target, parameter);
  }

}
