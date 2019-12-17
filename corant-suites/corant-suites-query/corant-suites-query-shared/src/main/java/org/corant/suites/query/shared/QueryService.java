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
package org.corant.suites.query.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * corant-suites-query
 *
 * @author bingo 下午10:33:30
 *
 */
public interface QueryService<Q, P> {

  /**
   * Step forward query, do not pagination, do not calculate the total number of records, can be
   * used for mass data streaming processing.
   *
   * @param <T> The expected query result set record type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The result set and whether there is a flag for the next result set.
   */
  <T> ForwardList<T> forward(Q q, P p);

  /**
   * Like {@link #forward(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result set record type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The result set and whether there is a flag for the next result set.
   */
  default <R, T> ForwardList<T> forward(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    ForwardList<R> raw = forward(q, p);
    if (raw == null) {
      return ForwardList.inst();
    } else {
      ForwardList<T> result = new ForwardList<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> f.apply(i, this)).collect(Collectors.toList()))
          .withHasNext(raw.hasNext);
    }
  }

  /**
   * Query a single result.
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The query result object.
   */
  <T> T get(Q q, P p);

  /**
   * Like {@link #get(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result object.
   */
  default <R, T> T get(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    R t = get(q, p);
    return f.apply(t, this);
  }

  /**
   * Paging query, returns the query result set and total record counts etc.
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The paging result list.
   */
  <T> PagedList<T> page(Q q, P p);

  /**
   * Like {@link #page(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result object.
   */
  default <R, T> PagedList<T> page(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    PagedList<R> raw = page(q, p);
    if (raw == null) {
      return PagedList.inst();
    } else {
      PagedList<T> result = new PagedList<>();
      return result
          .withResults(
              raw.getResults().stream().map(i -> f.apply(i, this)).collect(Collectors.toList()))
          .withTotal(raw.total);
    }
  }

  /**
   * Query list results
   *
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @return The query result list
   */
  <T> List<T> select(Q q, P p);

  /**
   * Like {@link #select(Object, Object)} but support an result record handler callback.
   *
   * @param <R> The orginal result record object type, usually is an Map.
   * @param <T> The expected query result object type.
   * @param q The query object reference, in named query this parameter may be the name of query.
   * @param p The query parameter, include query criteria and context.
   * @param f The result record handler callback.
   * @return The query result list
   */
  default <R, T> List<T> select(Q q, P p, BiFunction<R, QueryService<Q, P>, T> f) {
    List<R> raw = select(q, p);
    if (raw == null) {
      return new ArrayList<>();
    } else {
      return raw.stream().map(i -> f.apply(i, this)).collect(Collectors.toList());
    }
  }

  /**
   * Query stream results, use for mass data query.
   *
   * @param <T>
   * @param q
   * @param p
   * @return stream
   */
  <T> Stream<T> stream(Q q, P p);

  /**
   * corant-suites-query-shared
   *
   * Forward query result list, Consists of the result list and has next result mark.
   *
   * @author bingo 下午5:51:56
   *
   */
  public static class ForwardList<T> {

    private boolean hasNext;
    private final List<T> results = new ArrayList<>();

    ForwardList() {}

    public static <T> ForwardList<T> inst() {
      return new ForwardList<>();
    }

    public static <T> ForwardList<T> of(List<T> results, boolean hasNext) {
      ForwardList<T> il = new ForwardList<>();
      return il.withResults(results).withHasNext(hasNext);
    }

    /**
     * The result list
     */
    public List<T> getResults() {
      return results;
    }

    /**
     * Returns {@code true} if the query has more result set.
     *
     * @return hasNext
     */
    public boolean hasNext() {
      return hasNext;
    }

    /**
     * @return the hasNext
     */
    public boolean isHasNext() {
      return hasNext;
    }

    public ForwardList<T> withHasNext(boolean hasNext) {
      this.hasNext = hasNext;
      return this;
    }

    public ForwardList<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

  }

  /**
   * corant-suites-query-shared
   *
   * Paging query result list.
   *
   * @author bingo 下午6:11:55
   *
   */
  public static class PagedList<T> {

    private int total;
    private int pageSize;
    private int currentPage;
    private int totalPages;
    private int offset;
    private List<T> results = new ArrayList<>();

    public static <T> PagedList<T> inst() {
      return new PagedList<>();
    }

    public static <T> PagedList<T> of(int offset, int pageSize) {
      PagedList<T> pl = new PagedList<>();
      return pl.withOffset(offset).withPageSize(pageSize);
    }

    public static <T> PagedList<T> of(int total, List<T> results, int offset, int pageSize) {
      PagedList<T> pl = new PagedList<>();
      return pl.withResults(results).withTotal(total).withOffset(offset).withPageSize(pageSize);
    }

    /**
     * @return the currentPage
     */
    public int getCurrentPage() {
      return currentPage;
    }

    public int getOffset() {
      return offset;
    }

    /**
     * @return the pageSize
     */
    public int getPageSize() {
      return pageSize;
    }

    /**
     * @return the data
     */
    public List<T> getResults() {
      return results;
    }

    /**
     * @return the total
     */
    public int getTotal() {
      return total;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public PagedList<T> withOffset(int offset) {
      this.offset = offset;
      calPages();
      return this;
    }

    public PagedList<T> withPageSize(int pageSize) {
      this.pageSize = pageSize;
      calPages();
      return this;
    }

    public PagedList<T> withResults(List<T> results) {
      this.results.clear();
      if (results != null) {
        this.results.addAll(results);
      }
      return this;
    }

    public PagedList<T> withTotal(int total) {
      this.total = total;
      calPages();
      return this;
    }

    void calPages() {
      if (pageSize > 0 && offset >= 0) {
        if (total > 0) {
          totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        }
        int i = offset + 1;
        currentPage = i % pageSize == 0 ? i / pageSize : i / pageSize + 1;
      }
    }

  }
}
