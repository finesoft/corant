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
package org.corant.modules.cloud.alibaba.oss;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.config.declarative.ConfigInstances;
import com.aliyun.oss.OSSClientBuilder;

/**
 * corant-modules-cloud-alibaba
 *
 * @author bingo 上午11:29:13
 *
 */
public class OSSClientExtension implements Extension {

  protected OSSClientConfiguration config;

  void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
    if (config.isEnable()) {
      for (String bucket : config.getBuckets()) {
        event.addBean()
            .addQualifiers(Default.Literal.INSTANCE, Any.Literal.INSTANCE, NamedLiteral.of(bucket))
            .scope(ApplicationScoped.class).addTransitiveTypeClosure(OSSStorageService.class)
            .beanClass(OSSStorageService.class)
            .produceWith(beans -> new OSSStorageService(bucket, new OSSClientBuilder()
                .build(config.getEndpoint(), config.getAccessKeyId(), config.getSecretAccessKey())))
            .destroyWith((b, ctx) -> b.destroy());
      }
    }
  }

  void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    config = ConfigInstances.resolveSingle(OSSClientConfiguration.class);
  }
}
