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
package org.corant.modules.query;

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Maps.newHashMap;
import static org.corant.shared.util.Objects.max;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午9:16:07
 */
public interface QueryParameter extends Serializable {

  String OFFSET_PARAM_NME = "_offset";
  String LIMIT_PARAM_NME = "_limit";
  String CONTEXT_NME = "_context";

  // Use to manually adjust the query execution, can be set in the context
  String CTX_QHH_EXCLUDE_FETCH_QUERY = "__QHH_EXCLUDE_FETCH_QUERY";
  String CTX_QHH_EXCLUDE_RESULT_HINT = "__QHH_EXCLUDE_RESULT_HINT";
  String CTX_QHH_DONT_CONVERT_RESULT = "__QHH_DONT_CONVERT_RESULT";

  /**
   * Returns the query context that may be contained current caller context, the context information
   * may include the security context information of the caller, etc.
   *
   * @return the context maps
   */
  default Map<String, Object> getContext() {
    return Collections.emptyMap();
  }

  /**
   * Returns the query criteria, the criteria can be a Map or a POJO that are used to construct
   * query conditions or specifications.
   *
   * @return the criteria object or null
   */
  Object getCriteria();

  /**
   * Returns the expected number of query result set, usually used for queries with expected bounds
   * in the result set, such as paging queries({@link QueryService#page(Object, Object)}) or
   * specified forwarding range queries({@link QueryService#forward(Object, Object)}) or select
   * queries({@link QueryService#select(Object, Object)}) , but in streaming
   * queries({@link QueryService#stream(Object, Object)}), this value represents the size of the
   * result set fetched from the underlying database in each iteration.
   *
   * @return The expected number of query result set or the expected size of the result set of each
   *         iteration of the streaming query.
   */
  default Integer getLimit() {
    return null;
  }

  /**
   * Returns non-null and greater and equals 0 offset. Used for query result handling or specific
   * database query instructions, means skips the offset rows before beginning to return the rows.
   * Default return 0.
   *
   * @return the offset
   */
  default Integer getOffset() {
    return 0;
  }

  /**
   * corant-modules-query-api
   *
   * <p>
   * The default query parameter use for internal query handing. All pass in query parameter will be
   * converted to this.
   *
   * @author bingo 下午4:09:56
   *
   */
  class DefaultQueryParameter implements QueryParameter {

    private static final long serialVersionUID = 6618232487063961660L;

    protected Object criteria;
    protected Integer limit;
    protected Integer offset = 0;
    protected Map<String, Object> context = new HashMap<>();

    public DefaultQueryParameter() {}

    public DefaultQueryParameter(QueryParameter other) {
      if (other != null) {
        criteria = other.getCriteria();
        limit(other.getLimit());
        offset(other.getOffset());
        if (other.getContext() != null) {
          context.putAll(other.getContext());
        }
      }
    }

    public DefaultQueryParameter context(Map<String, Object> context) {
      this.context = newHashMap(context);
      return this;
    }

    public DefaultQueryParameter context(Object... objects) {
      context = mapOf(objects);
      return this;
    }

    public DefaultQueryParameter criteria(Object criteria) {
      this.criteria = criteria;
      return this;
    }

    @Override
    public Map<String, Object> getContext() {
      return context;
    }

    @Override
    public Object getCriteria() {
      return criteria;
    }

    @Override
    public Integer getLimit() {
      return limit;
    }

    @Override
    public Integer getOffset() {
      return offset;
    }

    public DefaultQueryParameter limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public DefaultQueryParameter offset(Integer offset) {
      this.offset = offset == null ? 0 : max(offset, 0);
      return this;
    }

  }

}
