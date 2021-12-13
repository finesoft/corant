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
package org.corant.modules.jpa.shared;

import static org.corant.context.Beans.select;
import static org.corant.modules.jpa.shared.JPQLHelper.getTotalQuery;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Primitives.isSimpleClass;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.SPACE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.inject.Any;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.ManagedType;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.Converters;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Primitives;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午4:07:53
 *
 */
public class JPAQueries {

  static final Logger logger = Logger.getLogger(JPAQueries.class.getName());

  static final Set<Class<?>> persistenceClasses =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  private JPAQueries() {}

  /**
   * Convert single tuple to target type object.
   * <p>
   * Note: If the given tuple only contains one element and the target type is simple class, for
   * example: Long/String etc. then the system default converter will be used.
   *
   * @param <T> the target type
   * @param converter the converter
   * @param tuple the tuple
   * @param type the target class
   *
   * @see Primitives#isSimpleClass(Class)
   * @see Conversions#toObject(Object, Class)
   */
  public static <T> T convertTuple(Converter<Tuple, T> converter, Tuple tuple, Class<T> type) {
    List<TupleElement<?>> eles = tuple.getElements();
    if (sizeOf(eles) == 1 && isSimpleClass(type)) {
      return toObject(tuple.get(0), type);
    }
    return converter.apply(tuple, null);
  }

  /**
   * Convert tuples to target type object list.
   *
   * <p>
   * Note: If the given tuple only contains one element and the target type is simple class, for
   * example: Long/String etc. then the system default converter will be used.
   *
   * @param <T> the target type
   * @param converter the converter
   * @param tuples the tuples
   * @param type the target class
   *
   * @see Primitives#isSimpleClass(Class)
   * @see Conversions#toObject(Object, Class)
   */
  public static <T> List<T> convertTuples(Converter<Tuple, T> converter, List<Tuple> tuples,
      Class<T> type) {
    List<T> results = null;
    if (isNotEmpty(tuples)) {
      results = new ArrayList<>(tuples.size());
      List<TupleElement<?>> eles = tuples.get(0).getElements();
      if (eles.size() == 1 && isSimpleClass(type)) {
        for (Tuple tuple : tuples) {
          results.add(toObject(tuple.get(0), type));
        }
      } else {
        for (Tuple tuple : tuples) {
          results.add(converter.apply(tuple, null));
        }
      }
    }
    return defaultObject(results, ArrayList::new);
  }

  /**
   * Create an instance of JPAQuery for executing a named query(in the Jakarta Persistence query
   * language or in native SQL).
   *
   * @param name the name of a query defined in metadata
   * @return JPAQuery
   * @see EntityManager#createNamedQuery(String)
   */
  public static AdvancedJPAQuery namedQuery(final String name) {
    return new AdvancedJPAQuery() {
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

  /**
   * Create an instance of TypedJPAQuery for executing a Jakarta Persistence query language named
   * query. The record of the result list of the query must be assignable to the type specified by
   * the resultClass argument.
   * <p>
   * Note: If the given type class is not a persistence class and there exists a Tuple to Object
   * converter, this method will use Tuple as the type of the query result and before return the
   * result to caller, the result will be converted to the given type through the converter. This is
   * very useful if the named query is a native query, but may throw exception if the named query is
   * not a native query. We recommend that use a persistence class as result class when the named
   * query is not a native query.
   *
   * @param name the name of a query defined in metadata
   * @param type the type of the query result
   * @return TypedJPAQuery
   * @see EntityManager#createNamedQuery(String,Class)
   */
  public static <T> TypedJPAQuery<T> namedQuery(final String name, final Class<T> type) {
    Converter<Tuple, T> converter;
    if (isPersistenceClass(type)
        || (converter = Converters.lookup(Tuple.class, type).orElse(null)) == null) {
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
    } else {
      return new TypedJPAQuery<>(type, converter) {

        @Override
        public String toString() {
          return "Named: " + name + getParameterDescription();
        }

        @Override
        protected Query createQuery() {
          return entityManagerSupplier.get().createNamedQuery(name, Tuple.class);
        }
      };
    }
  }

  /**
   * Create an instance of AbstractJPAQuery for executing a stored procedure in the database.
   *
   * Parameters must be registered before the stored procedure can be executed.
   *
   * If the stored procedure returns one or more result sets,any result set will be returned as a
   * list of type Object[].
   *
   *
   * @param name name assigned to the stored procedure query in metadata
   * @return JPAQuery
   * @see EntityManager#createNamedStoredProcedureQuery(String)
   */
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

  /**
   * Create an instance of JPAQuery for executing a native SQL statement, e.g., query execution will
   * result in each row of the SQL result being returned as a result of type Object[] (or a result
   * of type Object if there is only one column in the select list.) Column values are returned in
   * the order of their appearance in the select list and default JDBC type mappings are applied.
   *
   *
   * @param sqlString a native SQL query string
   * @return nativeQuery
   * @see EntityManager#createNativeQuery(String)
   */
  public static AdvancedJPAQuery nativeQuery(final String sqlString) {
    return new AdvancedJPAQuery() {
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

  /**
   * Create an instance of TypedJPAQuery for executing a native SQL query.
   * <p>
   * Note:
   * <p>
   * If the given type is JPA entity class or there is no converter which implements
   * javax.persistence.Tuple To Object conversion, call
   * {@link EntityManager#createNativeQuery(String, Class)} directly.
   *
   * <p>
   * If the given type is not JPA entity class (for example: SomeDTO) and there is a converter which
   * implements javax.persistence.Tuple To Object conversion then use javax.persistence.Tuple.class
   * as the result type and call {@link EntityManager#createNativeQuery(String, Class)} , and then
   * convert the result to the given type before the result is returned.
   *
   * @param <T> the type of the resulting instance(s)
   * @param sqlString a native SQL query string
   * @param type the class of the resulting instance(s)
   * @return nativeQuery
   *
   * @see EntityManager#createNativeQuery(String, Class)
   */
  public static <T> TypedJPAQuery<T> nativeQuery(final String sqlString, final Class<T> type) {
    Converter<Tuple, T> converter;
    if (isPersistenceClass(type)
        || (converter = Converters.lookup(Tuple.class, type).orElse(null)) == null) {
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
      return new TypedJPAQuery<>(type, converter) {

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

  /**
   * Create an instance of JPAQuery for executing a native SQL query.
   *
   * @param sqlString a native SQL query string
   * @param resultSetMapping the name of the result set mapping
   *
   * @see EntityManager#createNativeQuery(String, String)
   */
  public static AdvancedJPAQuery nativeQuery(final String sqlString,
      final String resultSetMapping) {
    return new AdvancedJPAQuery() {
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

  /**
   * Create an instance of TypedJPAQuery for executing a criteria query.
   *
   * @param <T> the type of the resulting instance(s)
   * @param criteriaQuery a criteria query object
   * @see EntityManager#createQuery(CriteriaQuery)
   */
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

  /**
   * Create an instance of JPAQuery for executing a Jakarta Persistence query language statement.
   *
   * @param qlString the Jakarta Persistence query language statement
   * @return query the JPAQuery
   * @see EntityManager#createQuery(String)
   */
  public static AdvancedJPAQuery query(final String qlString) {
    return new AdvancedJPAQuery() {
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

  /**
   * Create an instance of TypedJPAQuery for executing a Jakarta Persistence query language
   * statement. The record of the result list of the query must be assignable to the type specified
   * by the resultClass argument.
   *
   * @param <T> the type of the resulting instance(s)
   * @param qlString a Jakarta Persistence query string
   * @param type the type of the query result
   * @see EntityManager#createQuery(String, Class)
   */
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
          select(EntityManagerFactory.class, Any.Literal.INSTANCE)
              .forEach(emf -> emf.getMetamodel().getEntities().stream()
                  .map(ManagedType::getJavaType).forEach(persistenceClasses::add));
        }
      }
    }
    return persistenceClasses.contains(type);
  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午6:05:36
   *
   */
  public static abstract class AbstractQuery {

    protected ParameterBuilder parameterBuilder;
    protected Supplier<EntityManager> entityManagerSupplier;
    protected Map<Object, Object> hints;
    protected int maxResults = -1;
    protected int firstResult = -1;
    protected FlushModeType flushMode;
    protected LockModeType lockMode;

    protected void checkNoParametersConfigured() {
      if (parameterBuilder != null) {
        throw new IllegalArgumentException(
            "Cannot add parameters to a JPAQuerier which already has parameters configured");
      }
    }

    protected String getParameterDescription() {
      if (parameterBuilder == null) {
        return EMPTY;
      } else {
        return SPACE + parameterBuilder;
      }
    }

    protected <Q extends Query> Q populateQuery(Q query) {
      if (parameterBuilder != null) {
        parameterBuilder.populateQuery(entityManagerSupplier.get(), query);
      }
      if (hints != null) {
        hints.forEach((k, v) -> query.setHint(k.toString(), v));
      }
      if (firstResult > 0) {
        query.setFirstResult(firstResult);
      }
      if (maxResults > 0) {
        query.setMaxResults(maxResults);
      }
      if (flushMode != null) {
        query.setFlushMode(flushMode);
      }
      if (lockMode != null) {
        query.setLockMode(lockMode);
      }
      return query;
    }

    void setEntityManagerSupplier(final Supplier<EntityManager> entityManagerSupplier) {
      this.entityManagerSupplier =
          shouldNotNull(entityManagerSupplier, "The entity manager cannot null!");
    }

    void setFirstResult(int firstResult) {
      this.firstResult = max(firstResult, 0);
    }

    void setFlushMode(FlushModeType flushMode) {
      this.flushMode = flushMode;
    }

    void setHints(Object... hints) {
      this.hints = mapOf(hints);
    }

    void setLockMode(LockModeType lockMode) {
      this.lockMode = lockMode;
    }

    void setMaxResults(int maxResults) {
      this.maxResults = maxResults;
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

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午6:05:43
   *
   */
  public static abstract class AdvancedJPAQuery extends JPAQuery {

    @Override
    public AdvancedJPAQuery entityManager(final EntityManager entityManager) {
      setEntityManagerSupplier(
          () -> shouldNotNull(entityManager, "The entity manager cannot null!"));
      return this;
    }

    @Override
    public AdvancedJPAQuery entityManager(final Supplier<EntityManager> entityManagerSupplier) {
      setEntityManagerSupplier(entityManagerSupplier);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery firstResult(int firstResult) {
      setFirstResult(firstResult);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery flushMode(FlushModeType flushMode) {
      setFlushMode(flushMode);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery hints(Object... hints) {
      setHints(hints);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery lockMode(LockModeType lockMode) {
      setLockMode(lockMode);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery maxResults(int maxResults) {
      setMaxResults(maxResults);
      return this;
    }

    /**
     * Execute SELECT query and return the paging result set, the paging result set contains paging
     * results and the total number of results in the query result.
     *
     * Note: If the offset exceeds the total number of results in the query result, the total number
     * of returned paging result set results is 0.
     *
     * @param <T> the result object type
     * @param offset the position of the first result to retrieve.
     * @param limit the maximum number of results to retrieve.
     */
    @SuppressWarnings("unchecked")
    public <T> PagingResultSet<T> page(int offset, int limit) {
      PagingResultSet<T> rs = new PagingResultSet<>();
      rs.offset = max(offset, 0);
      rs.limit = max(limit, 1);
      Query query = populateQuery(createQuery());
      query.setFirstResult(rs.offset).setMaxResults(rs.limit);
      rs.results = defaultObject(query.getResultList(), ArrayList::new);
      int size = rs.results.size();
      if (size > 0) {
        if (size < limit) {
          rs.total = rs.offset + size;
        } else {
          rs.total = ((Number) populateQuery(getTotalQuery(query, entityManagerSupplier.get()))
              .setMaxResults(1).getSingleResult()).intValue();
        }
      }
      return rs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery parameters(List<?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery parameters(final Map<?, ?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvancedJPAQuery parameters(final Object... parameters) {
      setParameters(parameters);
      return this;
    }

  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午6:05:43
   *
   */
  public static abstract class JPAQuery extends AbstractQuery {

    public JPAQuery entityManager(final EntityManager entityManager) {
      setEntityManagerSupplier(
          () -> shouldNotNull(entityManager, "The entity manager cannot null!"));
      return this;
    }

    public JPAQuery entityManager(final Supplier<EntityManager> entityManagerSupplier) {
      setEntityManagerSupplier(entityManagerSupplier);
      return this;
    }

    /**
     * {@link Query#setFirstResult(int)}
     */
    public JPAQuery firstResult(int firstResult) {
      setFirstResult(firstResult);
      return this;
    }

    /**
     * {@link Query#setFlushMode(FlushModeType)}
     */
    public JPAQuery flushMode(FlushModeType flushMode) {
      setFlushMode(flushMode);
      return this;
    }

    /**
     * Execute a SELECT query that returns a single result.
     *
     * @param <T> the single result type
     * @return get
     */
    public <T> T get() {
      setMaxResults(1);
      List<T> results = this.select();
      if (isNotEmpty(results)) {
        return results.get(0);
      }
      return null;
    }

    /**
     * Set the query hint pairs.
     *
     * {@link Query#setHint(String, Object)}
     */
    public JPAQuery hints(Object... hints) {
      setHints(hints);
      return this;
    }

    /**
     * {@link Query#setLockMode(LockModeType)}
     */
    public JPAQuery lockMode(LockModeType lockMode) {
      setLockMode(lockMode);
      return this;
    }

    /**
     * {@link Query#setMaxResults(int)}
     */
    public JPAQuery maxResults(int maxResults) {
      setMaxResults(maxResults);
      return this;
    }

    /**
     * Bind positional query parameters, the query parameter position same as the given parameters
     * index.
     *
     * @param parameters positional parameters
     * @see Query#setParameter(int, Object)
     */
    public JPAQuery parameters(List<?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Bind named query parameters, the query parameter name same as the key of the given
     * parameters.
     *
     * @param parameters named parameters
     * @return parameters
     * @see Query#setParameter(String, Object)
     */
    public JPAQuery parameters(final Map<?, ?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Bind positional query parameters, the query parameter position same as the given parameters
     * array index.
     *
     * @param parameters positional parameters
     * @see Query#setParameter(int, Object)
     */
    public JPAQuery parameters(final Object... parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Execute a SELECT query and return the query results as List.
     *
     * @param <T> the result record type
     * @return select
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> select() {
      return defaultObject(populateQuery(createQuery()).getResultList(), ArrayList::new);
    }

    /**
     * Execute a SELECT query and return the query results as an untyped java.util.stream.Stream.By
     * default this method delegates to getResultList().stream(),however persistence provider may
     * choose to override this method to provide additional capabilities.
     *
     * @param <T> the type of the resulting instance(s)
     * @return a stream of the results
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<T> stream() {
      return populateQuery(createQuery()).getResultStream();
    }

    protected abstract Query createQuery();
  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午3:13:23
   *
   */
  public static class PagingResultSet<T> {
    protected int total;
    protected int offset;
    protected int limit;
    protected List<T> results;

    public int getLimit() {
      return limit;
    }

    public int getOffset() {
      return offset;
    }

    public List<T> getResults() {
      return results;
    }

    public int getTotal() {
      return total;
    }

  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午6:05:50
   *
   */
  public static abstract class TypedJPAQuery<T> extends AbstractQuery {

    final Class<T> resultType;
    final Converter<Tuple, T> converter;

    protected TypedJPAQuery() {
      resultType = null;
      converter = null;
    }

    protected TypedJPAQuery(Class<T> resultType, Converter<Tuple, T> converter) {
      this.resultType = resultType;
      this.converter = converter;
    }

    public TypedJPAQuery<T> entityManager(final EntityManager entityManager) {
      setEntityManagerSupplier(
          () -> shouldNotNull(entityManager, "The entity manager cannot null!"));
      return this;
    }

    public TypedJPAQuery<T> entityManager(final Supplier<EntityManager> entityManagerSupplier) {
      setEntityManagerSupplier(entityManagerSupplier);
      return this;
    }

    /**
     * {@link Query#setFirstResult(int)}
     */
    public TypedJPAQuery<T> firstResult(int firstResult) {
      setFirstResult(firstResult);
      return this;
    }

    /**
     * {@link Query#setFlushMode(FlushModeType)}
     */
    public TypedJPAQuery<T> flushMode(FlushModeType flushMode) {
      setFlushMode(flushMode);
      return this;
    }

    /**
     * Execute a SELECT query that returns a single typed result.
     *
     * @return the result object
     */
    public T get() {
      setMaxResults(1);
      List<T> results = this.select();
      if (isNotEmpty(results)) {
        return results.get(0);
      }
      return null;
    }

    /**
     * Set the query hint pairs.
     *
     * {@link Query#setHint(String, Object)}
     */
    public TypedJPAQuery<T> hints(Object... hints) {
      setHints(hints);
      return this;
    }

    /**
     * {@link Query#setLockMode(LockModeType)}
     */
    public TypedJPAQuery<T> lockMode(LockModeType lockMode) {
      setLockMode(lockMode);
      return this;
    }

    /**
     * {@link Query#setMaxResults(int)}
     */
    public TypedJPAQuery<T> maxResults(int maxResults) {
      setMaxResults(maxResults);
      return this;
    }

    /**
     * Execute SELECT query and return the paging result set, the paging result set contains paging
     * results and the total number of results in the query result.
     *
     * Note: If the offset exceeds the total number of results in the query result, the total number
     * of returned paging result set results is 0.
     *
     * @param offset the position of the first result to retrieve.
     * @param limit the maximum number of results to retrieve.
     */
    @SuppressWarnings("unchecked")
    public PagingResultSet<T> page(int offset, int limit) {
      PagingResultSet<T> rs = new PagingResultSet<>();
      rs.offset = max(offset, 0);
      rs.limit = max(limit, 1);
      Query query = populateQuery(createQuery());
      query.setFirstResult(rs.offset).setMaxResults(rs.limit);
      if (resultType == null || converter == null) {
        rs.results = defaultObject(query.getResultList(), ArrayList::new);
      } else {
        rs.results = convertTuples(converter, query.getResultList(), resultType);
      }
      int size = rs.results.size();
      if (size > 0) {
        if (size < limit) {
          rs.total = rs.offset + size;
        } else {
          rs.total = ((Number) populateQuery(getTotalQuery(query, entityManagerSupplier.get()))
              .setMaxResults(1).getSingleResult()).intValue();
        }
      }
      return rs;
    }

    /**
     * Bind positional query parameters, the query parameter position same as the given parameters
     * index.
     *
     * @param parameters positional parameters
     * @see Query#setParameter(int, Object)
     */
    public TypedJPAQuery<T> parameters(List<?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Bind named query parameters, the query parameter name same as the key of the given
     * parameters.
     *
     * @param parameters named parameters
     * @return parameters
     * @see Query#setParameter(String, Object)
     */
    public TypedJPAQuery<T> parameters(final Map<?, ?> parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Bind positional query parameters, the query parameter position same as the given parameters
     * array index.
     *
     * @param parameters positional parameters
     * @see Query#setParameter(int, Object)
     */
    public TypedJPAQuery<T> parameters(final Object... parameters) {
      setParameters(parameters);
      return this;
    }

    /**
     * Execute a SELECT query and return the query results as an typed List.
     *
     *
     * @return a list of the results
     */
    @SuppressWarnings("unchecked")
    public List<T> select() {
      if (resultType == null || converter == null) {
        return defaultObject(populateQuery(createQuery()).getResultList(), ArrayList::new);
      } else {
        return convertTuples(converter, populateQuery(createQuery()).getResultList(), resultType);
      }
    }

    /**
     * Execute a SELECT query and return the query results as an untyped java.util.stream.Stream.By
     * default this method delegates to getResultList().stream(),however persistence provider may
     * choose to override this method to provide additional capabilities.
     *
     * @return a stream of the results
     */
    @SuppressWarnings("unchecked")
    public Stream<T> stream() {
      if (resultType == null || converter == null) {
        return populateQuery(createQuery()).getResultStream();
      } else {
        Stream<Tuple> results = populateQuery(createQuery()).getResultStream();
        return results != null ? results.map(result -> convertTuple(converter, result, resultType))
            : Stream.empty();
      }
    }

    protected abstract Query createQuery();
  }

  abstract static class ParameterBuilder {
    public abstract void populateQuery(EntityManager entityManager, Query query);
  }
}
