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
package org.corant.asosat.ddd.domain.model;

import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.trim;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.shared.Archivable;
import org.corant.asosat.ddd.domain.shared.ArchiveInfo;
import org.corant.asosat.ddd.domain.shared.Nameable;
import org.corant.asosat.ddd.domain.shared.Numbered;
import org.corant.asosat.ddd.domain.shared.Param;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午1:40:55
 *
 */
@MappedSuperclass
public abstract class AbstractReferenceDataAggregation<P, T extends AbstractReferenceDataAggregation<P, T>>
    extends AbstractMannedAggregation<P, T> implements Nameable, Numbered, Archivable<P, T> {

  private static final long serialVersionUID = 7969451042668674966L;

  @Column
  private String name;

  @Column
  private String number;

  @Column(length = 2048)
  private String remark;

  @Embedded
  private ArchiveInfo archiveInfo = ArchiveInfo.empty();

  public AbstractReferenceDataAggregation() {
    super();
  }

  public AbstractReferenceDataAggregation(Participator creator) {
    super(creator);
  }

  public AbstractReferenceDataAggregation(Participator creator, String name) {
    this(creator);
    this.setName(name);
  }

  public AbstractReferenceDataAggregation(Participator creator, String name, String number) {
    this(creator, name);
    this.setNumber(number);
  }

  public AbstractReferenceDataAggregation(Participator creator, String name, String number,
      String description) {
    this(creator, name, number);
    this.setRemark(description);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void archive(P cmd, ArchiveHandler<P, T> handler) {
    if (!isArchived()) {
      if (handler != null) {
        handler.preArchive(cmd, (T) this);
      }
      this.setArchiveInfo(new ArchiveInfo((Param) cmd, true));
    }
  }

  @Override
  public void disable(P param, DisablingHandler<P, T> handler) {
    throw new NotSupportedException();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getNumber() {
    return this.number;
  }

  public String getRemark() {
    return this.remark;
  }

  @Override
  public ArchiveInfo obtainArchiveInfo() {
    return this.archiveInfo;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T revokeArchive(P cmd, RevokeArchiveHandler<P, T> handler) {
    T me = (T) this;
    if (isArchived()) {
      if (handler != null) {
        handler.preRevokeArchive(cmd, me);
      }
      this.setArchiveInfo(new ArchiveInfo((Param) cmd, false));
    }
    return me;
  }

  protected boolean changeName(String name) {
    if (isEquals(trim(name), this.name)) {
      return false;
    }
    this.setName(name);
    return true;
  }

  protected boolean changeNumber(String number) {
    if (isEquals(trim(number), this.number)) {
      return false;
    }
    this.setNumber(number);
    return true;
  }

  protected void changeRemark(String remark) {
    this.setRemark(remark);
  }

  protected void setArchiveInfo(ArchiveInfo archiveInfo) {
    this.archiveInfo = archiveInfo;
  }

  protected void setName(String name) {
    this.name = trim(name);
  }

  protected void setNumber(String number) {
    this.number = trim(number);
  }

  protected void setRemark(String remark) {
    this.remark = remark;
  }

}
