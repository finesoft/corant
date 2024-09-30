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
import static org.corant.shared.util.Configurations.getAssembledConfigValue;
import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.defaultBlank;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.corant.config.cdi.CurrentInjectionPoint;
import org.corant.context.AbstractBean;
import org.corant.context.Beans;
import org.corant.context.proxy.MethodInvoker;
import org.corant.context.proxy.ProxyBuilder;
import org.corant.context.qualifier.AutoCreated;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.QueryService;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.QueryService.QueryWay;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.modules.query.shared.QueryMappingService;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Configurations;
import org.corant.shared.util.Iterables;
import org.corant.shared.util.Methods;

/**
 * corant-modules-query-shared
 * <p>
 * Unfinished yet
 *
 * @author bingo 下午2:03:58
 */
public class DeclarativeQueryServiceDelegateBean extends AbstractBean<Object> {

  static final boolean useDeclaredMethod =
      getConfigValue("corant.query.declarative.use-declared-method", Boolean.TYPE, false);

  final Class<?> proxyType;

  public DeclarativeQueryServiceDelegateBean(BeanManager beanManager, Class<?> proxyType) {
    super(beanManager);
    this.proxyType = shouldNotNull(getUserClass(proxyType));
    qualifiers.add(AutoCreated.INST);
    qualifiers.add(Default.Literal.INSTANCE);
    qualifiers.add(Any.Literal.INSTANCE);
    types.add(proxyType);
    scope = ApplicationScoped.class;
    stereotypes.add(DeclarativeQueryService.class);
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext) {
    InjectionPoint ip = (InjectionPoint) beanManager
        .getInjectableReference(new CurrentInjectionPoint(), creationalContext);
    QueryTypeQualifier queryTypeQualifier = ip == null ? null
        : ip.getQualifiers().stream().filter(QueryTypeQualifier.class::isInstance)
            .map(QueryTypeQualifier.class::cast).findAny().orElse(null);
    return ProxyBuilder.buildContextual(beanManager, proxyType,
        m -> getExecution(m, queryTypeQualifier));
  }

  @Override
  public String getId() {
    return proxyType.getName();
  }

  @Override
  public String getName() {
    return proxyType.getName();
  }

  @SuppressWarnings({"rawtypes"})
  MethodInvoker createExecution(Method method, QueryTypeQualifier queryTypeQualifier) {
    final QueryMethod[] queryMethods = method.getAnnotationsByType(QueryMethod.class);
    final QueryMethod queryMethod = isNotEmpty(queryMethods) ? queryMethods[0] : null;
    String queryName =
        proxyType.getSimpleName().concat(Names.NAME_SPACE_SEPARATORS).concat(method.getName());
    QueryWay queryWay;
    if (queryMethod != null) {
      queryName = defaultBlank(getAssembledConfigValue(queryMethod.name()), queryName);
      queryWay = queryMethod.way();
    } else {
      queryWay = QueryWay.fromMethodName(method.getName());
    }
    final QueryService queryService = resolveQueryService(queryName, queryTypeQualifier);
    validateReturnType(queryName, method, queryWay);
    return createExecution(queryService, queryName, queryWay);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  MethodInvoker createExecution(QueryService queryService, String queryName, QueryWay queryWay) {
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

  MethodInvoker getExecution(Method method, QueryTypeQualifier queryTypeQualifier) {
    // TODO consider the bridge method "change" visibility of base class's methods ??
    if (!method.isDefault() && !method.isBridge() && !Modifier.isStatic(method.getModifiers())) {
      if (useDeclaredMethod) {
        if (Iterables.search(proxyType.getDeclaredMethods(), method) >= 0) {
          return createExecution(method, queryTypeQualifier);
        }
      } else {
        return createExecution(method, queryTypeQualifier);
      }
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  QueryService resolveQueryService(String queryName, QueryTypeQualifier queryTypeQualifier) {
    if (queryTypeQualifier == null) {
      return NamedQueryServiceManager.resolveQueryService(queryName);
    }
    QueryType type = queryTypeQualifier.type();
    String qualifier = Configurations.getAssembledConfigValue(queryTypeQualifier.qualifier());
    return shouldNotNull(NamedQueryServiceManager.tryResolveQueryService(type, qualifier),
        "Can't find any query service to execute declarative query %s %s %s.", proxyType, type,
        qualifier);
  }

  void validateReturnType(String queryName, Method method, QueryWay queryWay) {
    Class<?> returnClass = method.getReturnType();
    if (returnClass.equals(Void.TYPE) || returnClass.equals(Object.class)) {
      return;
    }
    Class<?>[] methodResultClass = null;
    Type returnType = method.getGenericReturnType();
    if (returnType instanceof ParameterizedType pt) {
      if (pt.getActualTypeArguments().length > 1) {
        throw new QueryRuntimeException(
            "Declarative query service [%s] method [%s] return type error, type mismatch!",
            proxyType, method);
      } else if (pt.getActualTypeArguments().length == 1) {
        if (pt.getActualTypeArguments()[0] instanceof Class<?> pc) {
          methodResultClass = new Class<?>[] {pc};
        } else if (pt.getActualTypeArguments()[0] instanceof WildcardType wpt) {
          // TODO FIXME check bounds
          if (wpt.getLowerBounds().length > 0) {
            methodResultClass = Arrays.stream(wpt.getLowerBounds()).filter(Class.class::isInstance)
                .map(x -> (Class<?>) x).toArray(Class[]::new);
          } else if (wpt.getUpperBounds().length > 0) {
            methodResultClass = Arrays.stream(wpt.getUpperBounds()).filter(Class.class::isInstance)
                .map(x -> (Class<?>) x).toArray(Class[]::new);
          }
        }
      }
      if (pt.getRawType() instanceof Class<?> rtc) {
        returnClass = rtc;
      }
    } else if (returnType instanceof Class<?> c) {
      methodResultClass = new Class<?>[] {c};
      returnClass = c;
    }

    if (queryWay == QueryWay.SELECT) {
      if (!Methods.isParameterTypesMatching(new Class[] {List.class}, new Class[] {returnClass},
          true, false)) {
        throw new QueryRuntimeException(
            "Declarative query service [%s] method [%s] return type error, the type must be a [%s]",
            proxyType, method, List.class.getName());
      }
      if (methodResultClass == null
          || (methodResultClass.length == 1 && methodResultClass[0].equals(returnClass))) {
        return;
      }
    } else if (queryWay == QueryWay.PAGE) {
      if (!Methods.isParameterTypesMatching(new Class[] {Paging.class}, new Class[] {returnClass},
          true, false)) {
        throw new QueryRuntimeException(
            "Declarative query service [%s] method [%s] return type error, the type must be a [%s]",
            proxyType, method, Paging.class.getName());
      }
      if (methodResultClass == null
          || (methodResultClass.length == 1 && methodResultClass[0].equals(returnClass))) {
        return;
      }
    } else if (queryWay == QueryWay.FORWARD) {
      if (!Methods.isParameterTypesMatching(new Class[] {Forwarding.class},
          new Class[] {returnClass}, true, false)) {
        throw new QueryRuntimeException(
            "Declarative query service [%s] method [%s] return type error, the type must be a [%s]",
            proxyType, method, Forwarding.class.getName());
      }
      if (methodResultClass == null
          || (methodResultClass.length == 1 && methodResultClass[0].equals(returnClass))) {
        return;
      }
    } else if (queryWay == QueryWay.STREAM) {
      if (!Methods.isParameterTypesMatching(new Class[] {Stream.class}, new Class[] {returnClass},
          true, false)) {
        throw new QueryRuntimeException(
            "Declarative query service [%s] method [%s] return type error, the type must be a [%s]",
            proxyType, method, List.class.getName());
      }
      if (methodResultClass == null
          || (methodResultClass.length == 1 && methodResultClass[0].equals(returnClass))) {
        return;
      }
    }
    if (isNotEmpty(methodResultClass)) {
      Class<?> queryResultClass =
          Beans.resolve(QueryMappingService.class).getQuery(queryName).getResultClass();
      if (queryResultClass != null) {
        for (Class<?> mrc : methodResultClass) {
          if (Methods.isParameterTypesMatching(new Class[] {queryResultClass}, new Class[] {mrc},
              true, false)) {
            return;
          }
        }
        throw new QueryRuntimeException(
            "Declarative query service:[%s] method:[%s] return type error, the type must be a [%s]",
            proxyType, method, queryResultClass.getName());
      }
    }
  }
}
