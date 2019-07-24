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
package org.corant.asosat.ddd.application.query;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.query.shared.spi.ParamReviser;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午10:59:43
 *
 */
@ApplicationScoped
@InfrastructureServices
public class SecurityContextParamReviser implements ParamReviser {

  @Override
  public void accept(Object queryName, Object param) {
    if (param instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> mapParam = Map.class.cast(param);
      if (Participator.currentUser().getId() != null) {
        mapParam.putIfAbsent(Participator.CURRENT_USER_ID_KEY, Participator.currentUser().getId());
      }
      if (Participator.currentOrg().getId() != null) {
        mapParam.putIfAbsent(Participator.CURRENT_ORG_ID_KEY, Participator.currentOrg().getId());
      }
    }
  }

}
