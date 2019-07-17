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

import java.beans.Transient;
import java.time.Instant;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.ddd.model.Aggregation;
import org.corant.suites.ddd.model.Aggregation.AggregationHandlerAdapter;

/**
 * @author bingo 下午6:45:49
 *
 */
public interface Confirmable<P, T extends Confirmable<P, T>> {

  /**
   * 确认
   */
  T confirm(P param, ConfirmHandler<P, T> handler);

  /**
   * 确认时间
   *
   * @return
   */
  default Instant getConfirmedTime() {
    return obtainConfirmationInfo() == null ? null : obtainConfirmationInfo().getConfirmedTime();
  }

  /**
   * 确认人id
   *
   * @return
   */
  default Participator getConfirmor() {
    return obtainConfirmationInfo() == null ? null : obtainConfirmationInfo().getConfirmor();
  }

  /**
   * 确认状态
   *
   * @return
   */
  default ConfirmationStatus getConfirmStatus() {
    return obtainConfirmationInfo() == null ? null : obtainConfirmationInfo().getStatus();
  }

  /**
   * 是否已经经过确认，表示已经确认同意或已经确认不同意
   *
   * @return
   */
  default boolean hasConfirmed() {
    return obtainConfirmationInfo() != null && obtainConfirmationInfo().hasConfirmed();
  }

  /**
   * 是否确认同意
   *
   * @return
   */
  default boolean isConfirmedApproved() {
    return obtainConfirmationInfo() != null && obtainConfirmationInfo().isApproved();
  }

  /**
   * 归档信息
   *
   * @return
   */
  @Transient
  default ConfirmationInfo obtainConfirmationInfo() {
    return ConfirmationInfo.EMPTY_INST;
  }

  /**
   * 撤销确认
   *
   * @param cmd
   */
  default T revokeConfirm(P cmd, RevokeConfirmHandler<P, T> handler) {
    throw new NotSupportedException();
  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:22:16
   *
   */
  public static abstract class ConfirmableAggregationHandlerAdapter<P, T extends Confirmable<P, T> & Aggregation>
      extends AggregationHandlerAdapter<P, T>
      implements RevokeConfirmHandler<P, T>, ConfirmHandler<P, T> {

    @Override
    public void preConfirm(T confirmable, P param, ConfirmationStatus confirmStatus) {

    }

    @Override
    public void preRevokeConfirm(T confirmable, P param) {

    }

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:22:16
   *
   */
  public static abstract class ConfirmableHandlerAdapter<P, T extends Confirmable<P, T>>
      extends ConfirmHandlerAdapter<P, T> implements RevokeConfirmHandler<P, T> {

    @Override
    public void preRevokeConfirm(T confirmable, P param) {

    }

  }

  /**
   *
   * @author bingo 下午7:00:22
   *
   */
  public enum ConfirmationStatus {
    UNCONFIRM(0), APPROVED(1), DISAPPROVED(2);

    int sign;

    private ConfirmationStatus(int sign) {
      this.sign = sign;
    }
  }

  /**
   * 确认处理器，可以用于是否能够确认，或者确认之后做什么之类的处理。<br/>
   *
   * <br>
   *
   * @author bingo 2014-8-24 <br>
   * @version
   */
  @FunctionalInterface
  public interface ConfirmHandler<P, T extends Confirmable<P, T>> {
    @SuppressWarnings("rawtypes")
    ConfirmHandler EMPTY_INST = (t, p, c) -> {
    };

    /**
     * 确认之前执行
     *
     * @param confirmable
     */
    void preConfirm(T confirmable, P param, ConfirmationStatus confirmStatus);

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:20:34
   *
   */
  public static abstract class ConfirmHandlerAdapter<P, T extends Confirmable<P, T>>
      implements ConfirmHandler<P, T> {

    @Override
    public void preConfirm(T confirmable, P param, ConfirmationStatus confirmStatus) {

    }

  }

  /**
   * 撤销确认处理器，可以用于是否能够撤销确认做什么之类的处理。
   *
   * @author bingo 2017年6月21日
   * @since
   */
  @FunctionalInterface
  public interface RevokeConfirmHandler<P, T extends Confirmable<P, T>> {
    @SuppressWarnings("rawtypes")
    RevokeConfirmHandler EMPTY_INST = (t, p) -> {
    };

    /**
     * 撤销确认之前执行
     *
     * @param confirmable
     */
    void preRevokeConfirm(T confirmable, P param);

  }

  /**
   * corant-asosat-ddd
   *
   * @author bingo 下午12:20:39
   *
   */
  public static abstract class RevokeConfirmHandlerAdapter<P, T extends Confirmable<P, T>>
      implements RevokeConfirmHandler<P, T> {

    @Override
    public void preRevokeConfirm(T confirmable, P param) {

    }

  }
}
