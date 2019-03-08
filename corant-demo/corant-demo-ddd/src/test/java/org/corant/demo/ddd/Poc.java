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
package org.corant.demo.ddd;

import static org.corant.shared.util.CollectionUtils.asList;
import java.math.BigDecimal;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.asosat.ddd.domain.shared.Param;
import org.corant.demo.ddd.domain.DemoValue;
import org.corant.demo.ddd.nosql.domain.NoSqlDemoAggregate;
import org.corant.demo.ddd.sql.domain.SqlDemoAggregate;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
import org.corant.suites.ddd.unitwork.JpaPersistenceService;
import org.corant.suites.jpa.hibernate.HibernateOrmSchemaUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * corant-demo-ddd
 *
 * @author bingo 下午7:28:05
 *
 */
@RunWith(CorantJUnit4ClassRunner.class)
public class Poc {

  @Inject
  JpaPersistenceService ps;

  public static void main(String... strings) {
    HibernateOrmSchemaUtils.stdoutRebuildSchema("");
  }

  @Test
  @Transactional
  public void saveNoSql() {
    // ps.toString();
    NoSqlDemoAggregate agg = new NoSqlDemoAggregate();
    agg.setBoolVal(Boolean.FALSE);
    agg.setDecVal(BigDecimal.valueOf(123));
    agg.setOneVal(new DemoValue("one", BigDecimal.ONE, Boolean.TRUE));
    agg.setStrVal("string");
    agg.setTimeVal(System.currentTimeMillis());
    agg.setSomeValues(asList(new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE)));
    agg.enable(Param.empty(), null);
  }

  @Test
  @Transactional
  public void saveSql() {
    // ps.toString();
    SqlDemoAggregate agg = new SqlDemoAggregate();
    agg.setBoolVal(Boolean.FALSE);
    agg.setDecVal(BigDecimal.valueOf(123));
    agg.setOneVal(new DemoValue("one", BigDecimal.ONE, Boolean.TRUE));
    agg.setStrVal("string");
    agg.setTimeVal(System.currentTimeMillis());
    agg.setSomeValues(asList(new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE),
        new DemoValue("one", BigDecimal.ONE, Boolean.TRUE)));
    agg.enable(Param.empty(), null);
  }
}
