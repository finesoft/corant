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
package org.corant.suites.ddd.repository;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.forceCast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午4:07:53
 *
 */
public class JPAQueies {

  static Logger logger = Logger.getLogger(JPAQueies.class.getName());

  public static JPAQuery namedQuery(final String name) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Named: " + name + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createNamedQuery(name);
      }
    };
  }

  public static <T> TypedJPAQuery<T> namedQuery(final String name, final Class<T> type) {
    return new TypedJPAQuery<>() {
      @Override
      public String toString() {
        return "Named: " + name + getParameterDescription();
      }

      @Override
      protected TypedQuery<T> createQuery() {
        return entityManagerSupplier.get().createNamedQuery(name, type);
      }
    };
  }

  public static JPAQuery namedStoredProcedureQuery(final String name) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Named stroed procedure query: " + name + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createNamedStoredProcedureQuery(name);
      }
    };
  }

  public static JPAQuery nativeQuery(final String sqlString) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Native query: " + sqlString + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createNativeQuery(sqlString);
      }
    };
  }

  public static <T> TypedJPAQuery<T> query(CriteriaQuery<T> criteriaQuery) {
    return new TypedJPAQuery<>() {
      @Override
      public String toString() {
        return "Query: " + getParameterDescription();
      }

      @Override
      protected TypedQuery<T> createQuery() {
        return entityManagerSupplier.get().createQuery(criteriaQuery);
      }
    };
  }

  public static JPAQuery query(final String qlString) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Query: " + qlString + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createQuery(qlString);
      }
    };
  }

  public static <T> TypedJPAQuery<T> query(final String qlString, final Class<T> type) {
    return new TypedJPAQuery<>() {
      @Override
      public String toString() {
        return "Query: " + qlString + getParameterDescription();
      }

      @Override
      protected TypedQuery<T> createQuery() {
        return entityManagerSupplier.get().createQuery(qlString, type);
      }
    };
  }

  public static JPAQuery storedProcedureQuery(final String procedureName) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Stored proceduce query: " + procedureName + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createStoredProcedureQuery(procedureName);
      }
    };
  }

  public static abstract class AbstractQuery {

    protected ParameterBuilder parameterBuilder;
    protected Supplier<EntityManager> entityManagerSupplier;

    protected void checkNoParametersConfigured() {
      if (parameterBuilder != null) {
        throw new IllegalArgumentException(
            "Cannot add parameters to a JPAQuerier which already has parameters configured");
      }
    }

    protected String getParameterDescription() {
      if (parameterBuilder == null) {
        return "";
      } else {
        return " " + parameterBuilder.toString();
      }
    }

    protected <Q extends Query> Q populateQuery(Q query) {
      if (parameterBuilder != null) {
        parameterBuilder.populateQuery(entityManagerSupplier.get(), query);
      }
      return query;
    }

    void setEntityManagerSupplier(final Supplier<EntityManager> entityManagerSupplier) {
      this.entityManagerSupplier =
          shouldNotNull(entityManagerSupplier, "The entity manager cannot null!");
    }

    void setParameters(Collection<?> parameters) {
      setParameters(parameters == null ? null : parameters.toArray());
    }

    void setParameters(final Map<?, ?> parameterMap) {
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
    }

    void setParameters(final Object... parameters) {
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
    }

  }

  public static abstract class JPAQuery extends AbstractQuery {

    public <T> T get() {
      return forceCast(populateQuery(createQuery()).getSingleResult());
    }

    public JPAQuery parameters(Collection<?> parameters) {
      setParameters(parameters);
      return this;
    }

    public JPAQuery parameters(final Map<?, ?> parameterMap) {
      setParameters(parameterMap);
      return this;
    }

    public JPAQuery parameters(final Object... parameters) {
      setParameters(parameters);
      return this;
    }

    public <T> List<T> select() {
      @SuppressWarnings("unchecked")
      List<T> resultList = populateQuery(createQuery()).getResultList();
      if (resultList == null) {
        resultList = new ArrayList<>();
      }
      return resultList;
    }

    protected abstract Query createQuery();

    protected JPAQuery entityManager(final EntityManager entityManager) {
      setEntityManagerSupplier(
          () -> shouldNotNull(entityManager, "The entity manager cannot null!"));
      return this;
    }

    protected JPAQuery entityManager(final Supplier<EntityManager> entityManagerSupplier) {
      setEntityManagerSupplier(entityManagerSupplier);
      return this;
    }
  }

  public static abstract class TypedJPAQuery<T> extends AbstractQuery {

    public T get() {
      TypedQuery<T> query = populateQuery(createQuery());
      return query.getSingleResult();
    }

    public TypedJPAQuery<T> parameters(Collection<?> parameters) {
      setParameters(parameters);
      return this;
    }

    public TypedJPAQuery<T> parameters(final Map<?, ?> parameterMap) {
      setParameters(parameterMap);
      return this;
    }

    public TypedJPAQuery<T> parameters(final Object... parameters) {
      setParameters(parameters);
      return this;
    }

    public List<T> select() {
      TypedQuery<T> query = populateQuery(createQuery());
      List<T> resultList = query.getResultList();
      if (resultList == null) {
        resultList = new ArrayList<>();
      }
      return resultList;
    }

    protected abstract TypedQuery<T> createQuery();

    protected TypedJPAQuery<T> entityManager(final EntityManager entityManager) {
      setEntityManagerSupplier(
          () -> shouldNotNull(entityManager, "The entity manager cannot null!"));
      return this;
    }

    protected TypedJPAQuery<T> entityManager(final Supplier<EntityManager> entityManagerSupplier) {
      setEntityManagerSupplier(entityManagerSupplier);
      return this;
    }
  }

  abstract static class ParameterBuilder {
    public abstract void populateQuery(EntityManager entityManager, Query query);
  }
}
