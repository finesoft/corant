package org.corant;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
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
 * corant-devops-test-pu
 *
 * @author bingo 下午3:18:48
 *
 */
@RunWith(CorantJUnit4ClassRunner.class)
public class CorantJunit4ClassRunnerTest2 {

  @Inject
  Logger logger;

  @Test
  public void testMultiThreads() throws InterruptedException {
    logger.info("test multi threads.");
    final CountDownLatch cdl = new CountDownLatch(1);
    new Thread(() -> {
      int loop = 0;
      while (loop < 1000) {
        loop += 100;
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
