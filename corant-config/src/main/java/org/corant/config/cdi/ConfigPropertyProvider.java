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

import static org.corant.kernel.normal.Names.NAME_SPACE_SEPARATORS;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import java.lang.reflect.Member;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.config.ConfigUtils;
import org.corant.config.CorantConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-config
 *
 * @author bingo 下午11:37:58
 *
 */
public class ConfigPropertyProvider {

  public static String getConfigKey(InjectionPoint injectionPoint) {
    ConfigProperty property =
        shouldNotNull(injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class));
    String key = defaultTrim(property.name());
    if (isBlank(key)) {
      Bean<?> bean = injectionPoint.getBean();
      Member member = injectionPoint.getMember();
      if (bean == null) {
        key = member.getDeclaringClass().getCanonicalName().replace('$', '.')
            .concat(NAME_SPACE_SEPARATORS).concat(ConfigUtils.dashify(member.getName()));
      } else {
        key = bean.getBeanClass().getCanonicalName().replace('$', '.').concat(NAME_SPACE_SEPARATORS)
            .concat(ConfigUtils.dashify(member.getName()));
      }
    }
    return key;
  }

  @ConfigProperty
  @Dependent
  public static Object getConfigProperty(InjectionPoint injectionPoint) {
    CorantConfig config = forceCast(ConfigProvider.getConfig());
    String key = defaultTrim(getConfigKey(injectionPoint));
    ConfigProperty property =
        shouldNotNull(injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class));
    Object result =
        config.getConversion().convert(config.getRawValue(key), injectionPoint.getType());
    if (result == null && property.defaultValue() != null
        && !property.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
      result = config.getConversion().convert(property.defaultValue(), injectionPoint.getType());
    }
    return config.getConversion().convertIfNecessary(result, injectionPoint.getType());
  }

}
