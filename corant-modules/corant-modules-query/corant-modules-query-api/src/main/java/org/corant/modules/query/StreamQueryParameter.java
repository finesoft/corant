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
package org.corant.modules.query;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.shared.retry.BackoffStrategy;
import org.corant.shared.retry.BackoffStrategy.FixedBackoffStrategy;

/**
 * corant-modules-query-api
 *
 * <p>
 * The query parameter object used for internal streaming query alone. In addition to the query
 * interface parameters, some parameters such as termination and retry are added. Since many
 * underlying streaming queries use batch queries, this class provides Condition adjustment
 * mechanism for batch query.
 *
 * @author bingo 下午3:14:48
 *
 */
public class StreamQueryParameter extends DefaultQueryParameter {

  private static final long serialVersionUID = -2111105679283097964L;

  static final Duration defRtyItl = Duration.ofSeconds(1L);

  protected int retryTimes = 0;

  protected BackoffStrategy retryBackoffStrategy = new FixedBackoffStrategy(defRtyItl);

  protected transient BiPredicate<Integer, Object> terminator;

  protected transient BiConsumer<Object, StreamQueryParameter> enhancer;

  protected boolean autoClose = false;

  public StreamQueryParameter() {}

  public StreamQueryParameter(QueryParameter other) {
    super(other);
  }

  public StreamQueryParameter(StreamQueryParameter other) {
    super(other);
    enhancer(other.enhancer).retryBackoffStrategy(other.retryBackoffStrategy)
        .retryTimes(other.retryTimes).terminator(other.terminator).autoClose(other.autoClose);
  }

  public StreamQueryParameter autoClose(boolean autoClose) {
    this.autoClose = autoClose;
    return this;
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
      offset(offset + getLimit());
    }
    return this;
  }

  public BiConsumer<Object, StreamQueryParameter> getEnhancer() {
    return enhancer;
  }

  @Override
  public Integer getLimit() {
    return defaultObject(super.getLimit(), 1);
  }

  /**
   * @see #retryBackoffStrategy(BackoffStrategy)
   */
  public BackoffStrategy getRetryBackoffStrategy() {
    return retryBackoffStrategy;
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
   * The terminator use to terminate the stream, if not set the stream ends naturally.
   *
   * @see #terminator(BiPredicate)
   * @return getTerminator
   */
  public BiPredicate<Integer, Object> getTerminator() {
    return terminator;
  }

  public boolean isAutoClose() {
    return autoClose;
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
   * Check whether to use retry mechanism, if the under query service implementation supports retry
   * then only {@link #getRetryTimes()} > 0 can use retry mechanism.
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
   * occurred, this method use to set the retry back-off strategy. The under query service
   * implementation may not support
   *
   * @param retryBackoffStrategy the retry back-off strategy, if given is null, the default interval
   *        is 2 seconds.
   */
  public StreamQueryParameter retryBackoffStrategy(BackoffStrategy retryBackoffStrategy) {
    this.retryBackoffStrategy =
        defaultObject(retryBackoffStrategy, () -> new FixedBackoffStrategy(Duration.ofSeconds(2L)));
    return this;
  }

  /**
   * The stream query may be use {@link QueryService#forward(Object, Object)} to fetch data in
   * batches, in this process the exception may be occurred, the query may retry after exception
   * occurred, this method use to set the retry times. The under query service implementation may
   * not support retry.
   *
   * @param retryTimes the retry times, if given is less than or equal to 0, it means no retry.
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
    return terminator != null && !terminator.test(counter, current);
  }

  /**
   * The terminator is used to terminate the stream. If it is not set, the stream will terminate
   * naturally. The terminator determines whether to terminate the stream by testing two parameters,
   * The first parameter is an integer that represents the number of objects that have flowed out,
   * The second parameter is an object that represents the last object that has flowed out.
   *
   * @param terminator the terminator to terminate the stream if the conditions are met.
   */
  public StreamQueryParameter terminator(BiPredicate<Integer, Object> terminator) {
    this.terminator = terminator;
    return this;
  }

}
