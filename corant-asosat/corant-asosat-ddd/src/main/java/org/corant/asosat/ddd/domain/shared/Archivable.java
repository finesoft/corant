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
package org.corant.asosat.ddd.domain.shared;

import java.time.Instant;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.ddd.model.Aggregation.AggregationHandlerAdapter;

/**
 * @author bingo 下午6:48:19
 *
 */
public interface Archivable<P, T extends Archivable<P, T>> {

  /**
   * 归档
   *
   * @param cmd
   * @param handler
   */
  void archive(P cmd, ArchiveHandler<P, T> handler);

  /**
   * 归档日志
   *
   * @return getArchivedLog
   */
  default String getArchivedLog() {
    return obtainArchiveInfo() == null ? null : obtainArchiveInfo().getArchivedLog();
  }

  /**
   * 归档日期
   *
   * @return
   */
  default Instant getArchivedTime() {
    return obtainArchiveInfo() == null ? null : obtainArchiveInfo().getArchivedTime();
  }

  /**
   * 归档人
   *
   * @return
   */
  default Participator getArchivist() {
    return obtainArchiveInfo() == null ? null : obtainArchiveInfo().getArchivist();
  }

  /**
   * 是否已归档
   *
   * @return
   */
  default boolean isArchived() {
    return obtainArchiveInfo() != null && obtainArchiveInfo().isArchived();
  }

  /**
   * 归档信息
   *
   * @return
   */
  ArchiveInfo obtainArchiveInfo();

  /**
   * 撤销归档
   *
   * @param cmd
   */
  default T revokeArchive(P cmd, RevokeArchiveHandler<P, T> handler) {
    throw new NotSupportedException();
  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:19:11
   *
   */
  public static abstract class ArchiableAggregationHandlerAdapter<P, T extends Archivable<P, T>, Aggregation>
      extends AggregationHandlerAdapter<P, T>
      implements ArchiveHandler<P, T>, RevokeArchiveHandler<P, T> {

    @Override
    public void preArchive(P cmd, T archivable) {

    }

    @Override
    public void preRevokeArchive(P cmd, T archivable) {

    }

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:22:16
   *
   */
  public static abstract class ArchiableHandlerAdapter<P, T extends Archivable<P, T>>
      extends ArchiveHandlerAdapter<P, T> implements RevokeArchiveHandler<P, T> {

    @Override
    public void preRevokeArchive(P cmd, T archivable) {

    }

  }

  /**
   * 归档处理器
   *
   * @author bingo
   *
   * @param <T>
   */
  @FunctionalInterface
  public interface ArchiveHandler<P, T extends Archivable<P, T>> {

    /**
     * 归档之前执行
     *
     * @param cmd
     * @param archivable
     */
    void preArchive(P cmd, T archivable);

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:16:55
   *
   */
  public static abstract class ArchiveHandlerAdapter<P, T extends Archivable<P, T>>
      implements ArchiveHandler<P, T> {

    @Override
    public void preArchive(P cmd, T archivable) {

    }

  }

  /**
   * 撤销归档处理器
   *
   * @author bingo 2017年6月21日
   * @since
   */
  @FunctionalInterface
  public interface RevokeArchiveHandler<P, T extends Archivable<P, T>> {

    /**
     * 撤销归档之前执行
     *
     * @param cmd
     * @param archivable
     */
    void preRevokeArchive(P cmd, T archivable);

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:17:00
   *
   */
  public static abstract class RevokeArchiveHandlerAdapter<P, T extends Archivable<P, T>>
      implements RevokeArchiveHandler<P, T> {

    @Override
    public void preRevokeArchive(P cmd, T archivable) {

    }

  }
}
