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
package org.corant.suites.query.shared.declarative;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StringUtils.defaultBlank;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.shared.normal.Names;
import org.corant.suites.cdi.AbstractBean;
import org.corant.suites.cdi.AutoCreated;
import org.corant.suites.cdi.Instances;
import org.corant.suites.cdi.proxy.ProxyBuilder;
import org.corant.suites.cdi.proxy.ProxyInvocationHandler.MethodInvoker;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.QueryService;
import org.corant.suites.query.shared.QueryService.QueryWay;

/**
 * corant-suites-query-shared
 *
 * Unfinish yet
 *
 * @author bingo 下午2:03:58
 *
 */
public class DeclarativeQueryServiceDelegateBean extends AbstractBean<Object> {

  static final Map<Method, MethodInvoker> methodInvokers = new ConcurrentHashMap<>();

  final Class<?> proxyType;

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
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext) {
    return ProxyBuilder.buildContextual(proxyType, beanManager, this::getExecution);
  }

  MethodInvoker getExecution(Method method) {
    return methodInvokers.computeIfAbsent(method, this::createExecution);
  }

  @SuppressWarnings({"rawtypes"})
  private MethodInvoker createExecution(Method method) {
    DeclarativeQueryService declaratives =
        shouldNotNull(proxyType.getDeclaredAnnotation(DeclarativeQueryService.class));
    final QueryService queryService = resolveQueryService(declaratives);
    final QueryMethod[] queryMethods = method.getAnnotationsByType(QueryMethod.class);
    final QueryMethod queryMethod = isNotEmpty(queryMethods) ? queryMethods[0] : null;
    String queryName =
        proxyType.getSimpleName().concat(Names.NAME_SPACE_SEPARATORS).concat(method.getName());
    QueryWay queryWay = QueryWay.SELECT;
    if (queryMethod != null) {
      queryName = defaultBlank(queryMethod.name(), queryName);
      queryWay = queryMethod.way();
    } else {
      queryWay = QueryWay.fromMethodName(queryName);
    }
    return createExecution(queryService, queryName, queryWay);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private MethodInvoker createExecution(QueryService queryService, String queryName,
      QueryWay queryWay) {
    if (queryWay == QueryWay.GET) {
      return (Object[] args) -> queryService.get(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.SELECT) {
      return (Object[] args) -> queryService.select(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.PAGE) {
      return (Object[] args) -> queryService.page(queryName, isEmpty(args) ? null : args[0]);
    } else if (queryWay == QueryWay.FORWARD) {
      return (Object[] args) -> queryService.forward(queryName, isEmpty(args) ? null : args[0]);
    } else {
      return (Object[] args) -> queryService.stream(queryName, isEmpty(args) ? null : args[0]);
    }
  }

  @SuppressWarnings("rawtypes")
  private QueryService resolveQueryService(DeclarativeQueryService declaratives) {
    AtomicReference<NamedQueryService> ref = new AtomicReference<>();
    Instances.select(NamedQueryServiceManager.class).forEach(nqs -> {
      if (nqs.getType() == declaratives.type()) {
        ref.set(nqs.get(declaratives.qualifier()));
      }
    });
    return shouldNotNull(ref.get(),
        "Can't find any query service to execute declarative query %s %s %s", proxyType,
        declaratives.type(), declaratives.qualifier());
  }

}
