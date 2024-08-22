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

import static org.corant.shared.util.Objects.forceCast;
import java.util.function.Function;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.lang.shared.ScriptEngineService;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.util.Services;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:55:32
 */
@Experimental
public class JavaScriptJaxrsQuerierBuilder extends AbstractNamedQuerierBuilder<JaxrsNamedQuerier> {

  protected static final ScriptEngineService scriptEngineService =
      Services.selectRequired(ScriptEngineService.class).findFirst()
          .orElseThrow(NotSupportedException::new);

  protected final Client client;
  protected final JaxrsNamedQueryClientConfig clientConfig;
  protected final Function<Object[], Object> execution;

  protected JavaScriptJaxrsQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler, Client client,
      JaxrsNamedQueryClientConfig clientConfig) {
    super(query, queryHandler, fetchQueryHandler);
    this.client = client;
    this.clientConfig = clientConfig;
    execution = scriptEngineService.createFunction(query.getScript().getCode(), "p");
  }

  @Override
  public DefaultJaxrsNamedQuerier build(QueryParameter queryParameter) {
    WebTarget target = client.target(clientConfig.getRoot());
    JaxrsQueryParameter parameter = forceCast(execution.apply(new Object[] {queryParameter}));
    parameter.postConstruct();
    return new DefaultJaxrsNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), clientConfig, target, parameter);
  }

}
