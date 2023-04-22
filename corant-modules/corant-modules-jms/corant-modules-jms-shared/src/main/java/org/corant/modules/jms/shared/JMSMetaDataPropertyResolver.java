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
package org.corant.modules.jms.shared;

import static org.corant.shared.util.Conversions.toObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.corant.config.Configs;
import org.corant.modules.jms.metadata.MetaDataPropertyResolver;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午8:52:09
 *
 */
@ApplicationScoped
public class JMSMetaDataPropertyResolver implements MetaDataPropertyResolver {

  @Override
  public <T> T resolve(String property, Class<T> clazz) {
    if (property != null && clazz != null) {
      return toObject(Configs.assemblyStringConfigProperty(property), clazz);
    }
    return toObject(property, clazz);
  }

}
