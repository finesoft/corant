package org.corant.modules.query.mongodb;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultMgNamedQuerierResolver extends AbstractNamedQuerierResolver<MgNamedQuerier> {

  protected final Map<String, FreemarkerMgQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Override
  public void onServiceInitialize() {
    onPreDestroy();
  }

  @Override
  public MgNamedQuerier resolve(String key, Object param) {
    FreemarkerMgQuerierBuilder builder = builders.computeIfAbsent(key, this::createBuilder);
    return builder.build(param);
  }

  protected FreemarkerMgQuerierBuilder createBuilder(String key) {
    Query query = getMappingService().getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not find name query for name [%s]", key);
    }
    switch (query.getScript().getType()) {
      case CDI:
      case JSE:
      case JS:
      case KT:
        throw new NotSupportedException("The query script type %s not support!",
            query.getScript().getType());
      default:
        return new FreemarkerMgQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler());
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    builders.clear();
    logger.fine(() -> "Clear default mongodb named querier resolver builders");
  }
}
