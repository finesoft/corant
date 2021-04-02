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
package org.corant.modules.query.shared;

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.corant.shared.util.Retry.RetryInterval;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午9:16:07
 *
 */
public interface QueryParameter extends Serializable {

  String OFFSET_PARAM_NME = "_offset";
  String LIMIT_PARAM_NME = "_limit";
  String CONTEXT_NME = "_context";

  /**
   * Return the query context that may be contain current user context or security context.
   *
   * @return getContext
   */
  default Map<String, Object> getContext() {
    return Collections.emptyMap();
  }

  /**
   * Return query criteria. The criteria can be Map or POJO that used to construct query conditions
   * or specifications for construct query conditions or specifications.
   *
   * @return getCriteria
   */
  Object getCriteria();

  /**
   * Returns the expected number of query result set, usually used for queries with expected bounds
   * in the result set, such as paging queries({@link QueryService#page(Object, Object)}) or
   * specified forwarding range queries({@link QueryService#forward(Object, Object)}) or select
   * queries({@link QueryService#select(Object, Object)}) , but in streaming
   * queries({@link QueryService#stream(Object, Object)}), this value represents the size of the
   * result set fetched from the underlying database in each iteration. Default is null, means that
   * if it is used in paging/forwarding/streaming query value is 16.
   *
   *
   * @return The expected number of query result set or the expected size of the result set of each
   *         iteration of the streaming query.
   */
  default Integer getLimit() {
    return null;
  }

  /**
   * Used for query result handling or specific database query instructions, means skips the offset
   * rows before beginning to return the rows. Default return 0.
   *
   * @return getOffset
   */
  default Integer getOffset() {
    return 0;
  }

  /**
   * corant-modules-query-shared
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
        limit = other.getLimit();
        offset = other.getOffset();
        if (other.getContext() != null) {
          context.putAll(other.getContext());
        }
      }
    }

    /**
     *
     * @param context the context to set
     */
    public DefaultQueryParameter context(Map<String, Object> context) {
      this.context = context;
      return this;
    }

    /**
     *
     * @param objects the context to set
     */
    public DefaultQueryParameter context(Object... objects) {
      context = mapOf(objects);
      return this;
    }

    /**
     *
     * @param criteria the criteria to set
     */
    public DefaultQueryParameter criteria(Object criteria) {
      this.criteria = criteria;
      return this;
    }

    /**
     *
     * @return the context
     */
    @Override
    public Map<String, Object> getContext() {
      return context;
    }

    /**
     *
     * @return the criteria
     */
    @Override
    public Object getCriteria() {
      return criteria;
    }

    /**
     *
     * @return the limit
     */
    @Override
    public Integer getLimit() {
      return limit;
    }

    /**
     *
     * @return the offset
     */
    @Override
    public Integer getOffset() {
      return offset;
    }

    /**
     *
     * @param limit the limit to set
     */
    public DefaultQueryParameter limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    /**
     *
     * @param offset the offset to set
     */
    public DefaultQueryParameter offset(Integer offset) {
      this.offset = max(offset, 0);
      return this;
    }

  }

  /**
   *
   * corant-modules-query-shared
   *
   * @author bingo 下午6:56:58
   *
   */
  class GenericQueryParameter<T> implements QueryParameter {
    private static final long serialVersionUID = 7809436027996029494L;
    protected T criteria;
    protected Integer limit;
    protected Integer offset = 0;
    protected Map<String, Object> context = new HashMap<>();

    public GenericQueryParameter() {}

    @JsonIgnore
    @Override
    public Map<String, Object> getContext() {
      return this.context;
    }

    @Override
    public T getCriteria() {
      return this.criteria;
    }

    @Override
    public Integer getLimit() {
      return this.limit;
    }

    @Override
    public Integer getOffset() {
      return this.offset;
    }

    public GenericQueryParameter<T> setContext(Map<String, Object> context) {
      this.context.clear();
      if (context != null) {
        this.context.putAll(context);
      }
      return this;
    }

    public GenericQueryParameter<T> setCriteria(T criteria) {
      this.criteria = criteria;
      return this;
    }

    public GenericQueryParameter<T> setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public GenericQueryParameter<T> setOffset(Integer offset) {
      this.offset = offset;
      return this;
    }
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:14:48
   *
   */
  class StreamQueryParameter extends DefaultQueryParameter {

    private static final long serialVersionUID = -2111105679283097964L;

    static final Duration defRtyItl = Duration.ofSeconds(1L);

    protected int retryTimes = 0;

    protected RetryInterval retryInterval = RetryInterval.noBackoff(defRtyItl);

    protected transient BiPredicate<Integer, Object> terminater;

    protected transient BiConsumer<Object, StreamQueryParameter> enhancer;

    public StreamQueryParameter() {}

    public StreamQueryParameter(QueryParameter other) {
      super(other);
    }

    public StreamQueryParameter(StreamQueryParameter other) {
      super(other);
      enhancer(other.enhancer).retryInterval(other.retryInterval).retryTimes(other.retryTimes)
          .retryInterval(other.retryInterval).terminater(other.terminater);
    }

    @Override
    public StreamQueryParameter context(Map<String, Object> context) {
      super.context(context);
      return this;
    }

    @Override
    public StreamQueryParameter context(Object... objects) {
      super.context(objects);
      return this;
    }

    @Override
    public StreamQueryParameter criteria(Object criteria) {
      super.criteria(criteria);
      return this;
    }

    public StreamQueryParameter enhancer(BiConsumer<Object, StreamQueryParameter> enhancer) {
      this.enhancer = enhancer;
      return this;
    }

    public StreamQueryParameter forward(Object current) {
      if (enhancer != null) {
        enhancer.accept(current, this);
      } else {
        offset(offset + super.getLimit());
      }
      return this;
    }

    public BiConsumer<Object, StreamQueryParameter> getEnhancer() {
      return enhancer;
    }

    /**
     * @see #retryInterval(RetryInterval)
     *
     * @return getRetryInterval
     */
    public RetryInterval getRetryInterval() {
      return retryInterval;
    }

    /**
     * @see #retryTimes(int)
     *
     * @return getRetryTimes
     */
    public int getRetryTimes() {
      return retryTimes;
    }

    /**
     * The terminater use to terminate the stream, if not set the stream ends naturally.
     *
     * @see #terminater(BiPredicate)
     * @return getTerminater
     */
    public BiPredicate<Integer, Object> getTerminater() {
      return terminater;
    }

    /**
     * The expected size of the result set of each iteration of the streaming query
     */
    @Override
    public StreamQueryParameter limit(Integer limit) {
      super.limit(limit);
      return this;
    }

    /**
     * Check whether to use retry mechanism, if the underly query service implemention supports
     * retry then only {@link #getRetryTimes()} > 0 can use retry mechanism.
     *
     * @return needRetry
     */
    public boolean needRetry() {
      return retryTimes > 0;
    }

    @Override
    public StreamQueryParameter offset(Integer offset) {
      super.offset(offset);
      return this;
    }

    /**
     * The stream query may be use {@link QueryService#forward(Object, Object)} to fetch data in
     * batches, in this process the exception may be occurred, the query may retry after exception
     * occurred, this method use to set the retry interval. The underly query service implemention
     * may not support
     *
     * @param retryInterval
     * @return retryInterval
     */
    public StreamQueryParameter retryInterval(RetryInterval retryInterval) {
      this.retryInterval =
          defaultObject(retryInterval, RetryInterval.noBackoff(Duration.ofMillis(2000L)));
      return this;
    }

    /**
     * The stream query may be use {@link QueryService#forward(Object, Object)} to fetch data in
     * batches, in this process the exception may be occurred, the query may retry after exception
     * occurred, this method use to set the retry times. The underly query service implemention may
     * not support retry.
     *
     * @param retryTimes
     * @return retryTimes
     */
    public StreamQueryParameter retryTimes(int retryTimes) {
      this.retryTimes = max(retryTimes, 0);
      return this;
    }

    /**
     * Check whether to terminate the stream
     *
     * @param counter the number of objects that have flowed out
     * @param current the last object that has flowed out
     * @return terminateIf
     */
    public boolean terminateIf(Integer counter, Object current) {
      return terminater != null && !terminater.test(counter, current);
    }

    /**
     *
     * The terminater is used to terminate the stream. If it is not set, the stream will terminate
     * naturally. The terminater determines whether to terminate the stream by testing two
     * parameters, The first parameter is an integer that represents the number of objects that have
     * flowed out, The second parameter is an object that represents the last object that has flowed
     * out.
     *
     * @param terminater
     * @return terminater
     */
    public StreamQueryParameter terminater(BiPredicate<Integer, Object> terminater) {
      this.terminater = terminater;
      return this;
    }
  }

}