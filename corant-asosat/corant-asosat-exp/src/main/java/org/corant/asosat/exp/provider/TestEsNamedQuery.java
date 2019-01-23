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
package org.corant.asosat.exp.provider;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.elastic.ElasticExtension;
import org.corant.suites.query.esquery.AbstractEsNamedQuery;
import org.corant.suites.query.esquery.DefaultEsQueryExecutor;

/**
 * corant-asosat-exp
 *
 * @author bingo 下午7:42:02
 *
 */
@ApplicationScoped
public class TestEsNamedQuery extends AbstractEsNamedQuery {

  @Inject
  ElasticExtension extension;

  @PostConstruct
  protected void onPostConstruct() {
    executor = new DefaultEsQueryExecutor(extension.getTransportClient("bingo"));
  }
}
