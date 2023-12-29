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
package org.corant.modules.elastic.data.service;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.elastic.data.ElasticExtension;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 上午11:05:18
 */
public class ElasticTransportClientService {

  @Inject
  protected ElasticExtension extension;

  @Inject
  @Any
  protected Instance<TransportClient> instance;// FIXME Unable to generate proxy

  public TransportClient get(String clusterName) {
    if (!instance.isUnsatisfied()) {
      return instance.select(NamedLiteral.of(Qualifiers.resolveName(clusterName))).get();
    }
    return null;
  }

  /**
   * @return the extension
   */
  public ElasticExtension getExtension() {
    return extension;
  }

}
