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
package org.corant.modules.rpc.feign;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.shared.util.Services;

/**
 * corant-modules-rpc-feign
 *
 * @author bingo 9:55:54
 *
 */
public class FeignExtension implements Extension {

  private final Set<Class<?>> feignClientBeanClasses = new LinkedHashSet<>();

  public void onAfterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery,
      final BeanManager beanManager) {
    for (Class<?> clientClass : feignClientBeanClasses) {
      afterBeanDiscovery.addBean(new FeignClientBean(beanManager, clientClass));
    }
  }

  public void onProcessAnnotatedType(
      @Observes @WithAnnotations(RegisterFeignClient.class) ProcessAnnotatedType<?> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    feignClientBeanClasses.add(pat.getAnnotatedType().getJavaClass());
  }
}
