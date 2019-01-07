/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.domain.shared;

import static org.corant.kernel.util.Preconditions.requireNotNull;
import java.time.Instant;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.shared.Confirmable.ConfirmationStatus;
import org.corant.suites.bundle.GlobalMessageCodes;

/**
 * @author bingo 上午11:17:22
 *
 */
@MappedSuperclass
@Embeddable
public class ConfirmationInfo implements OperationInfo {

  static final ConfirmationInfo EMPTY_INST = new ConfirmationInfo();
  public static final String KEY_CFM_TIME = "confirmedTime";
  public static final String KEY_CFM_STA = "confirmStatus";
  public static final String KEY_CFM_LOG = "confirmLog";
  public static final String KEY_CFMORID = "confirmorId";
  public static final String KEY_CFMORNME = "confirmorName";

  private static final long serialVersionUID = 8588019473110866431L;

  @Embedded
  @AttributeOverrides(value = {
      @AttributeOverride(column = @Column(name = "confirmorId"), name = "id"),
      @AttributeOverride(column = @Column(name = "confirmorName", length = 320), name = "name")})
  private Participator confirmor;

  /**
   * 是否经过确认
   */
  @Column(name = "confirmStatus", length = 32)
  @Enumerated(EnumType.STRING)
  private ConfirmationStatus status = ConfirmationStatus.UNCONFIRM;


  /**
   * 确认时间
   */
  @Column(name = "confirmedTime")
  private Instant confirmedTime;

  /**
   * 确认备注
   */
  @Column(name = "confirmLog", length = 2048)
  @Basic(fetch = FetchType.LAZY)
  private String confirmLog;

  public ConfirmationInfo() {

  }



  public ConfirmationInfo(ConfirmationStatus confirmStatus) {
    super();
    status = requireNotNull(confirmStatus, GlobalMessageCodes.ERR_PARAM);
    if (confirmStatus == ConfirmationStatus.APPROVED
        || confirmStatus == ConfirmationStatus.DISAPPROVED) {
      confirmedTime = Instant.now();
    }
  }

  public ConfirmationInfo(Param param) {
    this(param.getOperator(), param.getAttributes().getEnum(KEY_CFM_STA, ConfirmationStatus.class),
        param.getAttributes().getInstant(KEY_CFM_TIME, Instant.now()),
        param.getAttributes().getString(KEY_CFM_LOG));
  }

  public ConfirmationInfo(Participator confirmor, ConfirmationStatus confirmStatus) {
    this(confirmStatus);
    this.confirmor = confirmor;
  }

  public ConfirmationInfo(Participator confirmor, ConfirmationStatus confirmStatus,
      Instant confirmedTime, String confirmLog) {
    super();
    this.confirmor = confirmor;
    status = requireNotNull(confirmStatus, GlobalMessageCodes.ERR_PARAM);
    this.confirmedTime = confirmedTime;
    this.confirmLog = confirmLog;
  }

  public static ConfirmationInfo empty() {
    return EMPTY_INST;
  }

  /**
   * @return the confirmedTime
   */
  public Instant getConfirmedTime() {
    return confirmedTime;
  }

  /**
   * @return the info
   */
  public String getConfirmLog() {
    return confirmLog;
  }


  public Participator getConfirmor() {
    return confirmor == null ? Participator.EMPTY_INST : confirmor;
  }

  public ConfirmationStatus getStatus() {
    return status;
  }

  /**
   * 是否已经确认过，表示已经确认通过或者已经确认不通过
   *
   * @return
   */
  public boolean hasConfirmed() {
    return isApproved() || isDisapproved();
  }

  /**
   * 是否确认同意
   *
   * @return the confirmed
   */
  public boolean isApproved() {
    return status == ConfirmationStatus.APPROVED;
  }

  /**
   * 是否确认不同意
   *
   * @return
   */
  public boolean isDisapproved() {
    return status == ConfirmationStatus.DISAPPROVED;
  }


  public boolean isDone() {
    return confirmedTime != null;
  }

  @Override
  public Instant obtainOperatedTime() {
    return getConfirmedTime();
  }


  @Override
  public Long obtainOperatorId() {
    return getConfirmor().getId();
  }

  void init(Participator confirmor, ConfirmationStatus confirmStatus, Instant confirmedTime,
      String confirmLog) {
    this.confirmor = confirmor;
    status = requireNotNull(confirmStatus, GlobalMessageCodes.ERR_PARAM);
    this.confirmedTime = confirmedTime;
    this.confirmLog = confirmLog;
  }

}
