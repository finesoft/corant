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

import org.corant.asosat.ddd.domain.shared.Archivable.ArchiveHandler;
import org.corant.asosat.ddd.domain.shared.Archivable.RevokeArchiveHandler;
import org.corant.asosat.ddd.domain.shared.Param;
import org.corant.suites.ddd.model.Aggregation.DestroyHandler;
import org.corant.suites.ddd.model.Aggregation.EnablingHandler;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午5:14:26
 *
 */
public abstract class AbstractReferenceDataAggregationHandler<T extends AbstractReferenceDataAggregation<Param, T>>
    implements EnablingHandler<Param, T>, DestroyHandler<Param, T>, ArchiveHandler<Param, T>,
    RevokeArchiveHandler<Param, T> {

  @Override
  public void preArchive(Param cmd, T archivable) {}

  @Override
  public void preDestroy(Param param, T destroyable) {}

  @Override
  public void preEnable(Param param, T enabling) {}

  @Override
  public void preRevokeArchive(Param cmd, T archivable) {}

}

