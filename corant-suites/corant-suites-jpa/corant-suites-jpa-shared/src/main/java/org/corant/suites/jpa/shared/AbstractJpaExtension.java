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
package org.corant.suites.jpa.shared;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.persistence.spi.PersistenceUnitInfo;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午7:22:45
 *
 */
public abstract class AbstractJpaExtension implements Extension {

  private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<>();

  protected Map<String, PersistenceUnitInfo> getPersistenceUnitInfos() {
    return persistenceUnitInfos;
  }

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    JpaConfig cfg = JpaConfig.from(ConfigProvider.getConfig());
    cfg.getMetaDatas().forEach((n, pu) -> {
      persistenceUnitInfos.put(n, pu);
    });
  }

}
