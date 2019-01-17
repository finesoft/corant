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
package org.corant.suites.elastic;

import javax.ejb.ApplicationException;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:05:18
 *
 */
@ApplicationException
public class ElasticTransportClientService {

  @Inject
  @Any
  Instance<TransportClient> instance;

  public TransportClient get(String clusterName) {
    if (!instance.isUnsatisfied()) {
      return instance.select(NamedLiteral.of(clusterName)).get();
    }
    return null;
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent e) {
    // NOOP
  }
}
