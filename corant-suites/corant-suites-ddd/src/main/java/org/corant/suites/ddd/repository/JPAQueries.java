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

import static org.corant.context.Instances.select;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.isPrimitiveOrWrapper;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.ManagedType;
import org.corant.shared.conversion.Converters;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午4:07:53
 *
 */
public class JPAQueries {

  static Logger logger = Logger.getLogger(JPAQueries.class.getName());

  final static boolean hasTupleObjectConverter =
      Converters.lookup(Map.class, Object.class).isPresent();

  final static Set<Class<?>> persistenceClasses =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

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

  public static <T> TypedJPAQuery<T> nativeQuery(final String sqlString, final Class<T> type) {
    if (isPersistenceClass(type) || !hasTupleObjectConverter) {
      return new TypedJPAQuery<>() {
        @Override
        public String toString() {
          return "Native query: " + sqlString + getParameterDescription();
        }

        @Override
        protected Query createQuery() {
          return entityManagerSupplier.get().createNativeQuery(sqlString, type);
        }
      };
    } else {
      return new TypedJPAQuery<>(type) {

        @Override
        public String toString() {
          return "Native query: " + sqlString + getParameterDescription();
        }

        @Override
        protected Query createQuery() {
          return entityManagerSupplier.get().createNativeQuery(sqlString, Tuple.class);
        }

      };
    }
  }

  public static JPAQuery nativeQuery(final String sqlString, final String resultSetMapping) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Native query: " + sqlString + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createNativeQuery(sqlString, resultSetMapping);
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

  public static JPAQuery storedProcedureQuery(final String procedureName, final Class<?>... type) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Stored proceduce query: " + procedureName + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createStoredProcedureQuery(procedureName, type);
      }
    };
  }

  public static JPAQuery storedProcedureQuery(final String procedureName,
      final String... resultSetMappings) {
    return new JPAQuery() {
      @Override
      public String toString() {
        return "Stored proceduce query: " + procedureName + getParameterDescription();
      }

      @Override
      protected Query createQuery() {
        return entityManagerSupplier.get().createStoredProcedureQuery(procedureName,
            resultSetMappings);
      }
    };
  }

  static boolean isPersistenceClass(Class<?> type) {
    if (persistenceClasses.isEmpty()) {
      synchronized (JPAQueries.class) {
        if (persistenceClasses.isEmpty()) {
          select(EntityManagerFactory.class).forEach(emf -> {
            emf.getMetamodel().getEntities().stream().map(ManagedType::getJavaType)
                .forEach(persistenceClasses::add);
          });
        }
      }
    }
    return persistenceClasses.contains(type);
  }

  private static <T> T convertTuple(Tuple tuple, Class<T> type) {
    List<TupleElement<?>> eles = tuple.getElements();
    if (eles.size() == 1 && simpleClass(type)) {
      return toObject(tuple.get(0), type);
    }
    Map<String, Object> tupleMap = new LinkedHashMap<>(eles.size());
    for (TupleElement<?> e : eles) {
      tupleMap.put(e.getAlias(), tuple.get(e));
    }
    return toObject(tupleMap, type);
  }

  private static <T> List<T> convertTuples(List<Tuple> tuples, Class<T> type) {
    List<T> results = new ArrayList<>();
    if (tuples.size() > 0) {
      List<TupleElement<?>> eles = tuples.get(0).getElements();
      if (eles.size() == 1 && simpleClass(type)) {
        for (Tuple tuple : tuples) {
          results.add(toObject(tuple.get(0), type));
        }
      } else {
        for (Tuple tuple : tuples) {
          Map<String, Object> tupleMap = new LinkedHashMap<>(eles.size());
          for (TupleElement<?> e : eles) {
            tupleMap.put(e.getAlias(), tuple.get(e));
          }
          results.add(toObject(tupleMap, type));
        }
      }
    }
    return results;
  }

  private static boolean simpleClass(Class<?> type) {
    return isPrimitiveOrWrapper(type) || String.class.equals(type)
        || Number.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)
        || Temporal.class.isAssignableFrom(type) || Date.class.isAssignableFrom(type)
        || Enum.class.isAssignableFrom(type);
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

    final Class<T> resultType;

    protected TypedJPAQuery() {
      resultType = null;
    }

    protected TypedJPAQuery(Class<T> resultType) {
      this.resultType = resultType;
    }

    public T get() {
      Object result = populateQuery(createQuery()).getSingleResult();
      if (result == null) {
        return null;
      } else if (resultType == null || !hasTupleObjectConverter) {
        return forceCast(result);
      } else {
        Tuple tuple = (Tuple) result;
        return convertTuple(tuple, resultType);
      }
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

    @SuppressWarnings("unchecked")
    public List<T> select() {
      if (resultType == null || !hasTupleObjectConverter) {
        return defaultObject(populateQuery(createQuery()).getResultList(), ArrayList::new);
      } else {
        List<Tuple> resultList = populateQuery(createQuery()).getResultList();
        return resultList != null ? convertTuples(resultList, resultType) : new ArrayList<>();
      }
    }

    protected abstract Query createQuery();

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
