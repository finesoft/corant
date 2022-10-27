/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.ddd.shared.repository;

import static org.corant.context.Beans.findAnyway;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.context.qualifier.AutoCreated;
import org.corant.modules.ddd.Entity;
import org.corant.modules.ddd.shared.repository.AbstractTypedJPARepository.TypedJPARepositoryTemplate;
import org.corant.shared.ubiquity.Sortable;

@ApplicationScoped
public class TypedJPARepositoryProducer {

  protected TypedJPARepositoryFactory factory = TypedJPARepositoryFactory.DEFAULT;

  @PostConstruct
  protected void onPostConstruct() {
    Optional<TypedJPARepositoryFactory> custom = findAnyway(TypedJPARepositoryFactory.class);
    if (custom.isPresent()) {
      factory = custom.get();
    }
  }

  @SuppressWarnings("unchecked")
  @Produces
  @Dependent
  @AutoCreated
  <T extends Entity> TypedJPARepository<T> produceTypedJPARepository(InjectionPoint ip) {
    final Type type = ip.getType();
    final ParameterizedType parameterizedType = (ParameterizedType) type;
    final Type argType = parameterizedType.getActualTypeArguments()[0];
    final Class<T> entityClass = (Class<T>) argType;
    return factory.create(entityClass);
  }

  /**
   * corant-modules-ddd-shared
   * <p>
   * A CDI bean used to produce custom repository instance. The bean scope of implementation must be
   * ApplicationScope or Singleton.
   *
   * @author bingo 下午4:18:07
   *
   */
  @FunctionalInterface
  public interface TypedJPARepositoryFactory extends Sortable {

    TypedJPARepositoryFactory DEFAULT = TypedJPARepositoryTemplate::new;

    <T extends Entity> TypedJPARepository<T> create(Class<T> entityClass);
  }
}
