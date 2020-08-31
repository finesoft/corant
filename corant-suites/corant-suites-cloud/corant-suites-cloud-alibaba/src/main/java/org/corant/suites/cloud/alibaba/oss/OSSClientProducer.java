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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;

import static org.corant.shared.util.Strings.asDefaultString;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/8/31
 * @since
 */
@ApplicationScoped
public class OSSClientProducer {

  @Produces
  @OSSClient
  public OSS produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(OSSClient.class)) {
        qualifier = a;
        break;
      }
    }
    return new OSSClientBuilder()
        .build(getEndpoint(qualifier), getAccessKey(qualifier), getSecretKey(qualifier));
  }

  private String getAccessKey(Annotation qualifier) {
    if (qualifier instanceof OSSClient) {
      return ((OSSClient) qualifier).accessKey();
    } else {
      return asDefaultString(qualifier);
    }
  }

  private String getEndpoint(Annotation qualifier) {
    if (qualifier instanceof OSSClient) {
      return ((OSSClient) qualifier).endpoint();
    } else {
      return asDefaultString(qualifier);
    }
  }

  private String getSecretKey(Annotation qualifier) {
    if (qualifier instanceof OSSClient) {
      return ((OSSClient) qualifier).secretKey();
    } else {
      return asDefaultString(qualifier);
    }
  }
}
