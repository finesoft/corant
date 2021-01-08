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
package org.corant.config.cdi;

import static org.corant.shared.normal.Names.NAME_SPACE_SEPARATOR;
import static org.corant.shared.normal.Names.NAME_SPACE_SEPARATORS;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.isBlank;
import java.io.Serializable;
import java.lang.reflect.Member;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.config.CorantConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-config
 *
 * @author bingo 下午4:35:31
 *
 */
public class ConfigProducer implements Serializable {

  private static final long serialVersionUID = -7704094948781355258L;

  @ConfigProperty
  @Dependent
  public static Object getConfigProperty(InjectionPoint injectionPoint) {
    CorantConfig config = forceCast(ConfigProvider.getConfig());
    ConfigProperty property =
        shouldNotNull(injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class));
    String key = defaultTrim(property.name());
    if (isBlank(key)) {
      Bean<?> bean = injectionPoint.getBean();
      Member member = injectionPoint.getMember();
      if (bean == null) {
        key = member.getDeclaringClass().getCanonicalName().replace('$', NAME_SPACE_SEPARATOR)
            + NAME_SPACE_SEPARATORS + member.getName();
      } else {
        key = bean.getBeanClass().getCanonicalName().replace('$', NAME_SPACE_SEPARATOR)
            + NAME_SPACE_SEPARATORS + member.getName();
      }
    }
    final String useKey = key;
    return config.getConvertedValue(useKey, injectionPoint.getType(), property.defaultValue());
  }

  @Dependent
  @Produces
  public Config produce(InjectionPoint injectionPoint) {
    return ConfigProvider.getConfig();
  }
}
