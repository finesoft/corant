/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.shared;

import static org.corant.modules.query.shared.NamedQueryServiceManager.resolveQueryService;
import java.util.List;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.config.Configs;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午11:08:42
 */
@ApplicationScoped
@RequiredConfiguration(key = "corant.query.enable-forwarding-named-query-service",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
public class ForwardingNamedQueryService implements NamedQueryService {

  public static final boolean ENABLE =
      Configs.getValue("corant.query.enable-forwarding-named-query-service", Boolean.class, false);

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected QueryObjectMapper objectMapper;

  @Inject
  protected Instance<NamedQueryServiceManager> queryServiceManagers;

  @Override
  public <T> Forwarding<T> forward(String q, Object p) {
    return resolveQueryService(q).forward(q, p);
  }

  @Override
  public <T> T get(String q, Object p) {
    return resolveQueryService(q).get(q, p);
  }

  @Override
  public QueryObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public <T> Paging<T> page(String q, Object p) {
    return resolveQueryService(q).page(q, p);
  }

  @Override
  public <T> List<T> select(String q, Object p) {
    return resolveQueryService(q).select(q, p);
  }

  @Override
  public <T> Stream<T> stream(String q, Object p) {
    return resolveQueryService(q).stream(q, p);
  }

}
