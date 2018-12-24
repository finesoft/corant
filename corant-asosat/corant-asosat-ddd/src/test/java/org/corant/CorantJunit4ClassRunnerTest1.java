package org.corant;

import static org.corant.shared.util.MapUtils.getMapString;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
import org.corant.shared.normal.Names.JndiNames;
import org.junit.Test;
import org.junit.runner.RunWith;

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

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午3:18:48
 *
 */
@RunWith(CorantJUnit4ClassRunner.class)
// @Transactional
public class CorantJunit4ClassRunnerTest1 {

  @Inject
  @Named("dmmsRwDs")
  DataSource ds;

  @Inject
  InitialContext jndi;

  @Inject
  Logger logger;

  @Test
  public void testDataSource() throws SQLException {
    logger.info("test datasource " + ds);
    new QueryRunner(ds).query("SELECT * FROM CT_DMMS_INDU", new MapListHandler()).forEach(m -> {
      System.out.println(getMapString(m, "name"));
    });
  }

  @Test
  public void testDataSourceJndi() throws NamingException, SQLException {
    ds.getConnection();
    logger.info("test jndi datasource " + jndi.lookup(JndiNames.JNDI_DATS_NME + "/dmmsRwDs"));
  }

  @Test
  @Transactional
  public void testDataSourceUpdater() throws SQLException {
    String name = "汽车零部件制造业";
    System.out.println(new QueryRunner(ds).execute("UPDATE CT_DMMS_INDU SET NAME = ? WHERE id = ?",
        name, 379005035389915137l));
    // throw new CorantRuntimeException("==FALURE");
  }

  @Test
  public void testMultiThreads() throws InterruptedException {
    logger.info("test multi threads.");
    final CountDownLatch cdl = new CountDownLatch(1);
    new Thread(() -> {
      int loop = 0;
      while (loop < 1000) {
        loop += 500;
        logger.info(Thread.currentThread().getName() + "\t" + loop);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
      cdl.countDown();
    }).start();
    cdl.await();
    logger.info("finish multi threads works.");
  }

  @Test
  public void testSimple() {
    logger.info("test simple.");
  }


  @PostConstruct
  void testOnPostConstruct() {
    logger.info("test @PostConstruct.");
  }

  @PreDestroy
  void testOnPreDestroy() {
    logger.info("test @PreDestroy.");
  }
}
