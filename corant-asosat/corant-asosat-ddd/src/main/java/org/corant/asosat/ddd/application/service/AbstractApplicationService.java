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
package org.corant.asosat.ddd.application.service;

import static org.corant.kernel.util.Preconditions.requireNotNull;
import static org.corant.kernel.util.Preconditions.requireTrue;
import static org.corant.shared.util.MapUtils.getMapObject;
import java.util.Map;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.corant.suites.ddd.model.Entity.EntityReference;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:08:10
 *
 */
@ApplicationServices
public abstract class AbstractApplicationService implements ApplicationService {

  @Override
  public String getAppVerNum() {
    return null;
  }

  protected Participator currentOrg() {
    return Participator.currentOrg();
  }

  protected Participator currentOrg(Map<?, ?> cmd) {
    return getMapObject(cmd, Participator.CURRENT_ORG_KEY,
        v -> v == null ? null : (Participator) v);
  }

  protected Participator currentUser() {
    return Participator.currentUser();
  }

  protected Participator currentUser(Map<?, ?> cmd) {
    return getMapObject(cmd, Participator.CURRENT_USER_KEY,
        v -> v == null ? null : (Participator) v);
  }

  protected <T> T notNull(T obj) {
    return this.notNull(obj, GlobalMessageCodes.ERR_OBJ_NON_FUD);
  }

  protected <T> T notNull(T obj, String msgCode) {
    return this.notNull(obj, msgCode, new Object[0]);
  }

  protected <T> T notNull(T obj, String msgCode, Object... objects) {
    return requireNotNull(obj, msgCode, objects);
  }

  @SuppressWarnings("rawtypes")
  protected <T extends EntityReference> T validRefObj(T obj) {
    return requireTrue(obj, ObjectUtils::isNotNull, "");// FIXME
  }
}
