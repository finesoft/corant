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
package org.corant.modules.jpa.hibernate.orm;

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import org.corant.modules.jpa.shared.JPQLHelper;
import org.corant.modules.jpa.shared.JPQLHelper.JPQLResolver;
import org.hibernate.query.NativeQuery;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午3:55:53
 */
@Singleton
public class JPAQueryHelper implements JPQLResolver {

  @Override
  public Query resolveTotalQuery(Query noTotalQuery, EntityManager em) {
    @SuppressWarnings("rawtypes")
    org.hibernate.query.Query hq = shouldInstanceOf(noTotalQuery, org.hibernate.query.Query.class);
    if (noTotalQuery instanceof NativeQuery) {
      return em.createNativeQuery(JPQLHelper.getTotalQL(hq.getQueryString()));
    } else {
      return em.createQuery(JPQLHelper.getTotalQL(hq.getQueryString()));
    }
  }

}
