/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mail;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.corant.context.qualifier.Qualifiers;
import org.corant.shared.util.Services;

/**
 * corant-modules-mail
 *
 * @author bingo 14:22:48
 */
public class MailExtension implements Extension {

  public static final String PROPERTIES_PREFIX = "corant.mail.";
  public static final String PROPERTIES_PATTERN = "corant.mail*.host";

  protected Map<String, Properties> propertiesMap = new HashMap<>();

  protected final MailManager manager =
      Services.findPreferentially(MailManager.class).orElse(MailManager.DEFAULT_INSTANCE);

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null && isNotEmpty(propertiesMap)) {
      int size = sizeOf(propertiesMap);
      if (size == 1 && propertiesMap.keySet().iterator().next().isEmpty()) {
        event.<MailSender>addBean().addType(MailSender.class).beanClass(MailSender.class)
            .scope(Dependent.class)
            .produceWith(beans -> manager.resolveSender(propertiesMap.values().iterator().next()));
      } else if (size > 1) {
        Map<String, Annotation[]> qualifiers = Qualifiers.resolveNameds(propertiesMap.keySet());
        qualifiers.forEach((k, v) -> event.<MailSender>addBean().addQualifiers(v)
            .addType(MailSender.class).beanClass(MailSender.class).scope(Dependent.class)
            .produceWith(beans -> manager.resolveSender(propertiesMap.get(k))));
      }
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    propertiesMap = defaultObject(manager.resolveConfig(), HashMap::new);
  }
}
