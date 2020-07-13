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
package org.corant.suites.ddd.unitwork;

import static org.corant.suites.cdi.Instances.find;
import java.lang.annotation.Annotation;
import java.util.Optional;
import org.corant.suites.ddd.annotation.qualifier.JTAXA;
import org.corant.suites.ddd.annotation.qualifier.JTARL;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-ddd
 *
 * @author bingo 12:17:01
 *
 */
public class UnitOfWorks {

  static final boolean USE_JTA_XA_MODEL = ConfigProvider.getConfig()
      .getOptionalValue("ddd.unitofwork.use-xa", Boolean.class).orElse(Boolean.TRUE);

  public static Optional<AbstractJTAJPAUnitOfWork> currentDefaultUnitOfWork() {
    Optional<AbstractJTAJPAUnitOfWorksManager> uowm = currentDefaultUnitOfWorksManager();
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public static Optional<AbstractJTAJPAUnitOfWorksManager> currentDefaultUnitOfWorksManager() {
    return find(AbstractJTAJPAUnitOfWorksManager.class,
        USE_JTA_XA_MODEL ? JTAXA.INSTANCE : JTARL.INSTANCE);
  }

  public static Optional<UnitOfWork> currentUnitOfWork(Annotation... qualifiers) {
    Optional<UnitOfWorksManager> uowm = currentUnitOfWorksManager(qualifiers);
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public static Optional<UnitOfWorksManager> currentUnitOfWorksManager(Annotation... qualifiers) {
    return find(UnitOfWorksManager.class, qualifiers);
  }
}
