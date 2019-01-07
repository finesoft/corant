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
public abstract class AbstractReferenceDataAggregate<P, T extends AbstractReferenceDataAggregate<P, T>>
    extends AbstractMannedAggregate<P, T> implements Nameable, Numbered, Archivable<P, T> {

  private static final long serialVersionUID = 7969451042668674966L;

  @Column
  private String name;

  @Column
  private String number;

  @Column
  private String description;

  @Embedded
  private ArchiveInfo archiveInfo = ArchiveInfo.empty();

  public AbstractReferenceDataAggregate() {
    super();
  }

  public AbstractReferenceDataAggregate(Participator creator) {
    super(creator);
  }

  public AbstractReferenceDataAggregate(Participator creator, String name) {
    this(creator);
    this.setName(name);
  }

  public AbstractReferenceDataAggregate(Participator creator, String name, String number) {
    this(creator, name);
    this.setNumber(number);
  }

  public AbstractReferenceDataAggregate(Participator creator, String name, String number,
      String description) {
    this(creator, name, number);
    this.setDescription(description);
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
  public void destroy(P param, DestroyHandler<P, T> handler) {
    throw new NotSupportedException();
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getNumber() {
    return this.number;
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

  protected void changeDescription(String description) {
    this.setDescription(description);
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

  protected void setArchiveInfo(ArchiveInfo archiveInfo) {
    this.archiveInfo = archiveInfo;
  }

  protected void setDescription(String description) {
    this.description = description;
  }

  protected void setName(String name) {
    this.name = trim(name);
  }

  protected void setNumber(String number) {
    this.number = trim(number);
  }

}
