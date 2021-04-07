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
package com.github.wxpay.sdk;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.config.declarative.DeclarativeConfigKey;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-cloud-tencent
 *
 * @author bingo 上午11:40:28
 *
 */
@ApplicationScoped
public class CorantWXPayProvider {

  @Inject
  @DeclarativeConfigKey
  CorantWXPayConfig config;

  @Produces
  @Dependent
  WXPay produce(InjectionPoint ip) {
    try {
      return new WXPay(config, config.getNotifyUrl(), config.shouldAutoReport(),
          config.isUseSandbox());
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }
}
