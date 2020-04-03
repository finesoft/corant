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
package org.corant.suites.query.shared.cache;

import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryParameter;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午2:01:19
 *
 */
public class SimpleQueryCacheKey {
  final String queryName;
  final Object context;
  final Object criteria;
  final Integer limit;
  final Integer offset;
  final int hash;

  /**
   * @param queryName
   * @param context
   * @param criteria
   * @param limit
   * @param offset
   */
  SimpleQueryCacheKey(String queryName, Object context, Object criteria, Integer limit,
      Integer offset) {
    super();
    this.queryName = queryName;
    this.context = context;
    this.criteria = criteria;
    this.limit = limit;
    this.offset = offset;
    hash = calHashCode();
  }

  public static SimpleQueryCacheKey of(Querier querier) {
    String queryName = querier.getQuery().getName();
    QueryParameter queryParameter = querier.getQueryParameter();
    return new SimpleQueryCacheKey(queryName, queryParameter.getContext(),
        queryParameter.getCriteria(), queryParameter.getLimit(), queryParameter.getOffset());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SimpleQueryCacheKey other = (SimpleQueryCacheKey) obj;
    if (context == null) {
      if (other.context != null) {
        return false;
      }
    } else if (!context.equals(other.context)) {
      return false;
    }
    if (criteria == null) {
      if (other.criteria != null) {
        return false;
      }
    } else if (!criteria.equals(other.criteria)) {
      return false;
    }
    if (limit == null) {
      if (other.limit != null) {
        return false;
      }
    } else if (!limit.equals(other.limit)) {
      return false;
    }
    if (offset == null) {
      if (other.offset != null) {
        return false;
      }
    } else if (!offset.equals(other.offset)) {
      return false;
    }
    if (queryName == null) {
      if (other.queryName != null) {
        return false;
      }
    } else if (!queryName.equals(other.queryName)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  int calHashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (context == null ? 0 : context.hashCode());
    result = prime * result + (criteria == null ? 0 : criteria.hashCode());
    result = prime * result + (limit == null ? 0 : limit.hashCode());
    result = prime * result + (offset == null ? 0 : offset.hashCode());
    result = prime * result + (queryName == null ? 0 : queryName.hashCode());
    return result;
  }

}
