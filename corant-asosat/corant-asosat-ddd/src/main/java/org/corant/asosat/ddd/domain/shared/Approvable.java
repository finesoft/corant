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

import org.corant.shared.exception.NotSupportedException;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午7:22:44
 *
 */
public interface Approvable<P, T> {

  void approve(P param, ApproveHandler<P, T> handler);

  ApprovalStatus getApprovalStatus();

  default T revokeApprove(P cmd, RevokeApproveHandler<P, T> handler) {
    throw new NotSupportedException();
  }

  public enum ApprovalStatus {
    UNAPPROVE(0), APPROVING(1), APPROVED(2), DISAPPROVED(3);

    int sign;

    private ApprovalStatus(int sign) {
      this.sign = sign;
    }
  }

  @FunctionalInterface
  public interface ApproveHandler<P, T> {

    void preApprove(P cmd, T approvable);

  }

  public static abstract class ApproveHandlerAdapter<P, T> implements ApproveHandler<P, T> {

    @Override
    public void preApprove(P cmd, T approvable) {

    }

  }

  @FunctionalInterface
  public interface RevokeApproveHandler<P, T> {

    void preRevokeApprove(P cmd, T approvable);

  }

  public static abstract class RevokeApproveHandlerAdapter<P, T>
      implements RevokeApproveHandler<P, T> {

    @Override
    public void preRevokeApprove(P cmd, T approvable) {

    }

  }
}
