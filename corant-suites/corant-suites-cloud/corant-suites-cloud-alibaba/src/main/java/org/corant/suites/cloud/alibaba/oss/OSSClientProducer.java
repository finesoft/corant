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
package org.corant.suites.cloud.alibaba.oss;

import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.trim;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.config.Configs;
import org.corant.config.declarative.DeclarativeConfigResolver;
import org.corant.context.Naming;
import org.corant.shared.util.Strings;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/8/31
 * @since
 */
@ApplicationScoped
public class OSSClientProducer {

  Map<String, OSSClientConfiguration> configs = Collections.emptyMap();

  OSS build(OSSClientConfiguration config) {
    if (isNotBlank(config.getSecurityToken())) {
      return new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(),
          config.getSecretAccessKey(), config.getSecurityToken(), config);
    } else {
      return new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(),
          config.getSecretAccessKey(), config);
    }
  }

  @PostConstruct
  void onPostConstruct() {
    configs = DeclarativeConfigResolver.resolveMulti(OSSClientConfiguration.class);
  }

  @Produces
  @Naming
  OSS produce(InjectionPoint ip) {
    Naming naming = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(Naming.class)) {
        naming = (Naming) a;
        break;
      }
    }
    String name = Strings.EMPTY;
    if (naming != null) {
      name = trim(naming.value());
    }
    OSSClientConfiguration config = configs.get(Configs.assemblyStringConfigProperty(name));
    if (config != null) {
      return build(config);
    }
    return null;
  }
}
