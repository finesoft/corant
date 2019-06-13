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
package org.corant.suites.query.jpql;

import static org.corant.shared.util.ObjectUtils.forceCast;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;

/**
 * corant-suites-query-jpql
 *
 * @author bingo 下午12:13:02
 *
 */
public class DefaultJpqlQueryExecutor implements JpqlQueryExecutor {

  private EntityManagerFactory entityManagerFactory;

  /**
   * @param entityManagerFactory
   */
  public DefaultJpqlQueryExecutor(JpqlQueryConfiguration cfg) {
    super();
    entityManagerFactory = cfg.getEntityManagerFactory();
  }

  @Override
  public <T> T get(String jpql, Object... args) throws SQLException {
    return forceCast(createQuery(jpql, args).getSingleResult());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> select(String jpql, Object... args) throws SQLException {
    return createQuery(jpql, args).getResultList();
  }

  @Override
  public <T> Stream<T> stream(String jpql, Object... args) {
    return null;
  }

  protected Query createQuery(String sql, Object... args) {
    Query q =
        entityManagerFactory.createEntityManager(SynchronizationType.SYNCHRONIZED).createQuery(sql);
    int counter = 0;
    for (Object arg : args) {
      q.setParameter(counter++, arg);
    }
    return q;
  }
}
