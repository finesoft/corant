/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.exp.application;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import org.corant.suites.query.sqlquery.AbstractSqlNamedQuery;
import org.corant.suites.query.sqlquery.DefaultSqlQueryExecutor;
import org.corant.suites.query.sqlquery.SqlQueryConfiguration;
import org.corant.suites.query.sqlquery.dialect.MySQLDialect;

/**
 * corant-asosat-exp
 *
 * @author bingo 上午11:45:23
 *
 */
@ApplicationScoped
public class TestQueryService extends AbstractSqlNamedQuery {

  SqlQueryConfiguration configuration;

  @Inject
  @Named("dmmsRwDs")
  DataSource ds;

  @Override
  protected SqlQueryConfiguration getConfiguration() {
    return null;
  }

  @PostConstruct
  protected void onPostConstruct() {
    configuration =
        SqlQueryConfiguration.defaultBuilder().dataSource(ds).dialect(new MySQLDialect()).build();
    executor = new DefaultSqlQueryExecutor(configuration);
  }
}
