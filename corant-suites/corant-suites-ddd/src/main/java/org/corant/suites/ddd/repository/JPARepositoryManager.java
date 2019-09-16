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
package org.corant.suites.ddd.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.persistence.PersistenceContext;
import org.corant.kernel.api.PersistenceService.PersistenceContextLiteral;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午7:09:18
 *
 */
public class JPARepositoryManager {

  static final Map<PersistenceContext, JPARepository> respositories = new ConcurrentHashMap<>();

  @ApplicationScoped
  @Named
  JPARepository produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final Named named = annotated.getAnnotation(Named.class);
    String pun = named == null ? "" : named.value();
    PersistenceContext pc = PersistenceContextLiteral.of(pun);
    return respositories.computeIfAbsent(pc, (p) -> {
      return new AbstractJPARepository(pc) {};
    });
  }
}
