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
package org.corant.modules.query.shared.declarative;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.defaultBlank;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.config.Configs;
import org.corant.context.AbstractBean;
import org.corant.context.proxy.MethodInvoker;
import org.corant.context.proxy.ProxyBuilder;
import org.corant.context.qualifier.AutoCreated;
import org.corant.modules.query.QueryService;
import org.corant.modules.query.QueryService.QueryWay;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.shared.normal.Names;

/**
 * corant-modules-query-shared
 *
 * Unfinish yet
 *
 * @author bingo 下午2:03:58
 *
 */
public class DeclarativeQueryServiceDelegateBean extends AbstractBean<Object> {

  static final Map<Method, MethodInvoker> methodInvokers = new ConcurrentHashMap<>();

  final Class<?> proxyType;
  final String queryQualifier;
  final QueryType queryType;

  /**
   * @param beanManager
   */
  public DeclarativeQueryServiceDelegateBean(BeanManager beanManager, Class<?> proxyType) {
    super(beanManager);
    this.proxyType = shouldNotNull(getUserClass(proxyType));
    qualifiers.add(AutoCreated.INST);
    qualifiers.add(Default.Literal.INSTANCE);
    qualifiers.add(Any.Literal.INSTANCE);
    stereotypes.add(DeclarativeQueryService.class);
    types.add(proxyType);
    scope = ApplicationScoped.class;
    DeclarativeQueryService declaratives =
        shouldNotNull(proxyType.getDeclaredAnnotation(DeclarativeQueryService.class));
    queryQualifier = Configs.resolveVariable(declaratives.qualifier());
    queryType = declaratives.type();
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext) {
    return ProxyBuilder.buildContextual(beanManager, proxyType, this::getExecution);
  }

  @Override
  public void destroy(Object instance, CreationalContext<Object> creationalContext) {
    methodInvokers.clear();
  }

  @Override
  public String getId() {
    return proxyType.getName();
  }

  @Override
  public String getName() {
    return proxyType.getName();
  }

  MethodInvoker getExecution(Method method) {
    return methodInvokers.computeIfAbsent(method, this::createExecution);
  }

  @SuppressWarnings({"rawtypes"})
  private MethodInvoker createExecution(Method method) {
    final QueryService queryService = resolveQueryService();
    final QueryMethod[] queryMethods = method.getAnnotationsByType(QueryMethod.class);
    final QueryMethod queryMethod = isNotEmpty(queryMethods) ? queryMethods[0] : null;
    String queryName =
        proxyType.getSimpleName().concat(Names.NAME_SPACE_SEPARATORS).concat(method.getName());
    QueryWay queryWay;
    if (queryMethod != null) {
      queryName = defaultBlank(Configs.resolveVariable(queryMethod.name()), queryName);
      queryWay = queryMethod.way();
    } else {
      queryWay = QueryWay.fromMethodName(method.getName());
    }
    return createExecution(queryService, queryName, queryWay);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private MethodInvoker createExecution(QueryService queryService, String queryName,
      QueryWay queryWay) {
    if (queryWay == QueryWay.GET) {
      return (target, args) -> queryService.get(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.SELECT) {
      return (target, args) -> queryService.select(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.PAGE) {
      return (target, args) -> queryService.page(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.FORWARD) {
      return (target, args) -> queryService.forward(queryName, isEmpty(args) ? null : args[0]);
    } else {
      return (target, args) -> queryService.stream(queryName, isEmpty(args) ? null : args[0]);
    }
  }

  @SuppressWarnings("rawtypes")
  private QueryService resolveQueryService() {
    return shouldNotNull(NamedQueryServiceManager.resolveQueryService(queryType, queryQualifier),
        "Can't find any query service to execute declarative query %s %s %s.", proxyType, queryType,
        queryQualifier);
  }

}
