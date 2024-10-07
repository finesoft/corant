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
package org.corant.modules.jpa.hibernate.orm;

import static java.lang.String.format;
import static java.util.Collections.addAll;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import org.corant.context.CDIs;
import org.corant.modules.jpa.shared.JPAEntityRepository;
import org.corant.shared.ubiquity.TypeLiteral;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.internal.QueryBinder;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 16:19:33
 */
public class HibernateAdditionalMappingContributor implements AdditionalMappingContributor {

  protected Logger logger = Logger.getLogger(HibernateAdditionalMappingContributor.class.getName());

  @Override
  public void contribute(AdditionalMappingContributions contributions,
      InFlightMetadataCollector metadata, ResourceStreamLocator resourceStreamLocator,
      MetadataBuildingContext buildingContext) {
    if (CDIs.isEnabled()) {
      Set<Class<?>> mappedClasses = metadata.getEntityBindings().stream()
          .map(PersistentClass::getMappedClass).collect(Collectors.toSet());
      Set<Class<?>> repositoryClasses = new LinkedHashSet<>();
      Type type = new TypeLiteral<JPAEntityRepository<?>>() {}.getType();
      CDI.current().getBeanManager().getBeans(type).stream()
          .filter(b -> mappedClasses.contains(resolvePersistenceClass(b.getBeanClass())))
          .forEach(b -> repositoryClasses.add(b.getBeanClass()));
      logger.info(format("Find %s JPA entity repository classes", repositoryClasses.size()));
      for (Class<?> clazz : repositoryClasses) {
        bindNamedQueryIfNecessary(clazz, buildingContext);
        bindNamedNativeQueryIfNecessary(clazz, buildingContext);
        bindNamedStoredProcedureQueryIfNecessary(clazz, buildingContext);
      }
    } else {
      logger.info("Can't bind name queries from repository, CDI not enabled");
    }
  }

  protected void bindNamedNativeQueryIfNecessary(Class<?> clazz,
      MetadataBuildingContext buildingContext) {
    Set<NamedNativeQuery> namedQueries = new LinkedHashSet<>();
    addAll(namedQueries, clazz.getAnnotationsByType(NamedNativeQuery.class));
    for (NamedNativeQueries nnqs : clazz.getAnnotationsByType(NamedNativeQueries.class)) {
      addAll(namedQueries, nnqs.value());
    }
    if (!namedQueries.isEmpty()) {
      for (NamedNativeQuery nq : namedQueries) {
        if (nq == null) {
          continue;
        }
        QueryBinder.bindNativeQuery(nq, buildingContext, false);
        logger.fine(format("Bind named native query:[%s] to metadata, annotated class:[%s]",
            nq.name(), clazz.getCanonicalName()));
      }
    }
  }

  protected void bindNamedQueryIfNecessary(Class<?> clazz,
      MetadataBuildingContext buildingContext) {
    Set<NamedQuery> namedQueries = new LinkedHashSet<>();
    addAll(namedQueries, clazz.getAnnotationsByType(NamedQuery.class));
    for (NamedQueries nqs : clazz.getAnnotationsByType(NamedQueries.class)) {
      addAll(namedQueries, nqs.value());
    }
    if (!namedQueries.isEmpty()) {
      for (NamedQuery nq : namedQueries) {
        if (nq == null) {
          continue;
        }
        QueryBinder.bindQuery(nq, buildingContext, false);
        logger.fine(format("Bind named query:[%s] to metadata, annotated class:[%s]", nq.name(),
            clazz.getCanonicalName()));
      }
    }
  }

  protected void bindNamedStoredProcedureQueryIfNecessary(Class<?> clazz,
      MetadataBuildingContext buildingContext) {
    Set<NamedStoredProcedureQuery> namedQueries = new LinkedHashSet<>();
    addAll(namedQueries, clazz.getAnnotationsByType(NamedStoredProcedureQuery.class));
    for (NamedStoredProcedureQueries nnqs : clazz
        .getAnnotationsByType(NamedStoredProcedureQueries.class)) {
      addAll(namedQueries, nnqs.value());
    }
    if (!namedQueries.isEmpty()) {
      for (NamedStoredProcedureQuery nq : namedQueries) {
        if (nq == null) {
          continue;
        }
        QueryBinder.bindNamedStoredProcedureQuery(nq, buildingContext, false);
        logger
            .fine(format("Bind named stored procedure query:[%s] to metadata, annotated class:[%s]",
                nq.name(), clazz.getCanonicalName()));
      }
    }
  }

  protected Class<?> resolvePersistenceClass(Class<?> clazz) {
    Class<?> resolvedClass = null;
    Class<?> repoClass = clazz;
    do {
      if (repoClass.getGenericSuperclass() instanceof ParameterizedType pt) {
        resolvedClass = (Class<?>) pt.getActualTypeArguments()[0];
        break;
      } else {
        Type[] genericInterfaces = repoClass.getGenericInterfaces();
        for (Type type : genericInterfaces) {
          if (type instanceof ParameterizedType parameterizedType
              && parameterizedType.getRawType() instanceof Class<?> rawClass
              && JPAEntityRepository.class.isAssignableFrom(rawClass)) {
            resolvedClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            break;
          }
        }
      }
    } while (resolvedClass == null && (repoClass = repoClass.getSuperclass()) != null);
    return resolvedClass;
  }

}
