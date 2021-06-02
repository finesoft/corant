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
package org.corant.modules.jms.metadata;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:35:09
 *
 */
public interface MetaDataPropertyResolver extends Sortable {

  static <T> T get(T property) {
    Instance<MetaDataPropertyResolver> inst = CDI.current().select(MetaDataPropertyResolver.class);
    if (!inst.isUnsatisfied()) {
      return inst.stream().sorted().findFirst().get().resolve(property);
    }
    return property;
  }

  <T> T resolve(T property);
}
