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
package org.corant.suites.query.mongodb.cdi;

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.kernel.api.DataSourceService;
import org.corant.suites.query.mongodb.AbstractMgNamedQueryService;
import org.corant.suites.query.shared.NamedQueryService;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@ApplicationScoped
public class MgNamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  DataSourceService dataSourceService;

  @Produces
  @ApplicationScoped
  @MgQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final MgQuery sc = shouldNotNull(annotated.getAnnotation(MgQuery.class));
    final String dataBase = shouldNotBlank(sc.value());
    return services.computeIfAbsent(dataBase, (ds) -> {
      return new AbstractMgNamedQueryService() {
        @Override
        protected MongoDatabase getDataBase() {
          return resolveNamed(MongoDatabase.class, ds).get();
        }
      };
    });
  }
}
