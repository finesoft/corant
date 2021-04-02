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
package org.corant.modules.ddd.unitwork;

import static org.corant.context.Instances.find;
import java.lang.annotation.Annotation;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.ddd.annotation.qualifier.JTARL;
import org.corant.modules.ddd.annotation.qualifier.JTAXA;
import org.corant.modules.ddd.annotation.stereotype.InfrastructureServices;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-ddd
 *
 * <p>
 * Used to provide the current unit of work manager or unit of work, If you do not apply the
 * existing mechanism, return to the custom unit of work instance by overriding this class.
 * </p>
 *
 * @author bingo 12:17:01
 *
 */
@ApplicationScoped
@InfrastructureServices
public class UnitOfWorks {

  @Inject
  @ConfigProperty(name = "corant.ddd.unitofwork.use-xa", defaultValue = "true")
  protected boolean useJtaXa;

  public Optional<AbstractJTAJPAUnitOfWork> currentDefaultUnitOfWork() {
    Optional<AbstractJTAJPAUnitOfWorksManager> uowm = currentDefaultUnitOfWorksManager();
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public Optional<AbstractJTAJPAUnitOfWorksManager> currentDefaultUnitOfWorksManager() {
    return find(AbstractJTAJPAUnitOfWorksManager.class, useJtaXa ? JTAXA.INSTANCE : JTARL.INSTANCE);
  }

  public Optional<UnitOfWork> currentUnitOfWork(Annotation... qualifiers) {
    Optional<UnitOfWorksManager> uowm = currentUnitOfWorksManager(qualifiers);
    return Optional.ofNullable(uowm.isPresent() ? uowm.get().getCurrentUnitOfWork() : null);
  }

  public Optional<UnitOfWorksManager> currentUnitOfWorksManager(Annotation... qualifiers) {
    return find(UnitOfWorksManager.class, qualifiers);
  }
}
