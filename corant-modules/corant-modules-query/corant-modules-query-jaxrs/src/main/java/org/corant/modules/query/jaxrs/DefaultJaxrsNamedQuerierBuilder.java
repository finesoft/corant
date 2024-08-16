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

import static java.util.Collections.emptyMap;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.getOptMapObject;
import static org.corant.shared.util.Maps.transformKey;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Map;
import java.util.Objects;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier.WebTargetConfig;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierBuilder;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 20:55:32
 */
public class DefaultJaxrsNamedQuerierBuilder
    extends AbstractNamedQuerierBuilder<DefaultJaxrsNamedQuerier> {

  protected final Client client;
  protected final JaxrsNamedQueryClientConfig clientConfig;
  protected final WebTargetConfig targetConfig;

  protected DefaultJaxrsNamedQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler, Client client, JaxrsNamedQueryClientConfig clientConfig,
      WebTargetConfig targetConfig) {
    super(query, queryHandler, fetchQueryHandler);
    this.client = client;
    this.clientConfig = clientConfig;
    this.targetConfig = targetConfig;
  }

  @SuppressWarnings("unchecked")
  @Override
  public DefaultJaxrsNamedQuerier build(QueryParameter queryParameter) {
    WebTarget target = client.target(clientConfig.getRoot());
    if (isNotBlank(targetConfig.getPath())) {
      target = target.path(targetConfig.getPath());
    }
    if (targetConfig.containsTempleOrParameter()) {
      Map<String, Object> context = defaultObject(queryParameter.getContext(), emptyMap());
      Map<String, Object> criteria = emptyMap();
      if (queryParameter.getCriteria() instanceof Map map) {
        criteria = transformKey(map, Objects::toString);
      } else if (queryParameter.getCriteria() != null) {
        criteria = getQueryHandler().getObjectMapper().mapOf(queryParameter.getCriteria(), true);
      }
      if (isNotEmpty(targetConfig.getEncodeSlashTemplateNames())) {
        for (String s : targetConfig.getEncodeSlashTemplateNames()) {
          target.resolveTemplate(s, getOptMapObject(criteria, s).orElse(context.get(s)), true);
        }
      }
      if (isNotEmpty(targetConfig.getTemplateNames())) {
        for (String s : targetConfig.getTemplateNames()) {
          target.resolveTemplate(s, getOptMapObject(criteria, s).orElse(context.get(s)));
        }
      }
      if (isNotEmpty(targetConfig.getFormEncodedTemplateNames())) {
        for (String s : targetConfig.getFormEncodedTemplateNames()) {
          target.resolveTemplateFromEncoded(s, getOptMapObject(criteria, s).orElse(context.get(s)));
        }
      }
      if (isNotEmpty(targetConfig.getQueryParameterNames())) {
        for (String s : targetConfig.getQueryParameterNames()) {
          target.queryParam(s, getOptMapObject(criteria, s).orElse(context.get(s)));
        }
      }
      if (isNotEmpty(targetConfig.getMatrixParameterNames())) {
        for (String s : targetConfig.getMatrixParameterNames()) {
          target.matrixParam(s, getOptMapObject(criteria, s).orElse(context.get(s)));
        }
      }
    }
    return new DefaultJaxrsNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), clientConfig, targetConfig, target);
  }

}
