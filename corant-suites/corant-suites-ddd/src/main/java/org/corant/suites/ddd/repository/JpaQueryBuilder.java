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
package org.corant.suites.ddd.repository;

import static org.corant.shared.util.ObjectUtils.asString;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午9:43:04
 *
 */
public abstract class JpaQueryBuilder {

  protected ParameterBuilder parameterBuilder;

  /**
   * Build name query builder
   */
  public static JpaQueryBuilder namedQuery(final String namedQuery) {
    return new JpaQueryBuilder() {
      @Override
      public String toString() {
        return "Named: " + namedQuery + getParameterDescription();
      }

      @Override
      protected Query createQuery(EntityManager entityManager) {
        return entityManager.createNamedQuery(namedQuery);
      }
    };
  }

  /**
   * Build native query with SQL statement builder
   */
  public static JpaQueryBuilder nativeQuery(final String nativeQuery) {
    return new JpaQueryBuilder() {
      @Override
      public String toString() {
        return "NativeQuery: " + nativeQuery + getParameterDescription();
      }

      @Override
      protected Query createQuery(EntityManager entityManager) {
        return entityManager.createNativeQuery(nativeQuery);
      }
    };
  }

  /**
   * Build JPQL query builder
   */
  public static JpaQueryBuilder query(final String query) {
    return new JpaQueryBuilder() {
      @Override
      public String toString() {
        return "Query: " + query + getParameterDescription();
      }

      @Override
      protected Query createQuery(EntityManager entityManager) {
        return entityManager.createQuery(query);
      }
    };
  }

  /**
   * Build JPA query object
   */
  public Query build(EntityManager entityManager) {
    Query query = createQuery(entityManager);
    populateQuery(entityManager, query);
    return query;
  }

  /**
   * Set the parameter's collection to the builder.
   */
  public JpaQueryBuilder parameters(Collection<?> parameters) {
    return this.parameters(parameters == null ? null : parameters.toArray());
  }

  /**
   * Set the parameter's map to the builder.
   */
  public JpaQueryBuilder parameters(final Map<?, ?> parameterMap) {
    checkNoParametersConfigured();
    parameterBuilder = new ParameterBuilder() {
      @Override
      public void populateQuery(EntityManager entityManager, Query query) {
        if (parameterMap != null) {
          for (Entry<?, ?> entry : parameterMap.entrySet()) {
            query.setParameter(asString(entry.getKey()), entry.getValue());
          }
        }
      }

      @Override
      public String toString() {
        return "Parameters: " + parameterMap;
      }
    };
    return this;
  }

  /**
   * Set the parameter's array to the builder.
   */
  public JpaQueryBuilder parameters(final Object... parameters) {
    checkNoParametersConfigured();
    parameterBuilder = new ParameterBuilder() {
      @Override
      public void populateQuery(EntityManager entityManager, Query query) {
        if (parameters != null) {
          int counter = 0;
          for (Object parameter : parameters) {
            query.setParameter(counter++, parameter);
          }
        }
      }

      @Override
      public String toString() {
        return "Parameters: " + Arrays.toString(parameters);
      }
    };
    return this;
  }

  protected void checkNoParametersConfigured() {
    if (parameterBuilder != null) {
      throw new IllegalArgumentException(
          "Cannot add parameters to a JpaQueryBuilder which already has parameters configured");
    }
  }

  protected abstract Query createQuery(EntityManager entityManager);

  protected String getParameterDescription() {
    if (parameterBuilder == null) {
      return "";
    } else {
      return " " + parameterBuilder.toString();
    }
  }

  protected void populateQuery(EntityManager entityManager, Query query) {
    if (parameterBuilder != null) {
      parameterBuilder.populateQuery(entityManager, query);
    }
  }

  /**
   *
   * Inner parameter builder
   *
   * @author bingo 2013年4月27日
   * @since 1.0
   */
  protected abstract static class ParameterBuilder {
    public abstract void populateQuery(EntityManager entityManager, Query query);
  }
}
