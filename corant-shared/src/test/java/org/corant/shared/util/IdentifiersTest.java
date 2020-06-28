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
package org.corant.shared.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.corant.shared.util.Identifiers.SnowflakeUUIDGenerator;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午7:49:51
 *
 */
public class IdentifiersTest extends TestCase {

  public static void main(String... strings) throws InterruptedException {
    int workers = 2, times = 9876, size = workers * times;
    final long[][] arr = new long[workers][times];
    ExecutorService es = Executors.newFixedThreadPool(workers);
    final CountDownLatch latch = new CountDownLatch(workers);
    for (int worker = 0; worker < workers; worker++) {
      final int workerId = worker;
      es.submit(() -> {
        for (int i = 0; i < times; i++) {
          arr[workerId][i] =
              Identifiers.snowflakeBufferUUID(workerId, true, System::currentTimeMillis);
        }
        latch.countDown();
      });
    }
    latch.await();
    Set<Long> set = new HashSet<>();
    Set<Long> timestamps = new HashSet<>();
    Map<Long, List<Long>> tmp = new LinkedHashMap<>();
    for (long[] ar : arr) {
      for (long a : ar) {
        set.add(a);
        long time = SnowflakeUUIDGenerator.parseGeningInstant(a).toEpochMilli();
        long woid = SnowflakeUUIDGenerator.parseGeningWorkerId(a);
        long seq = SnowflakeUUIDGenerator.parseGeningSequence(a);
        long dcid = SnowflakeUUIDGenerator.parseGeningDataCenterId(a);
        tmp.computeIfAbsent(time, k -> new ArrayList<>()).add(seq);
        timestamps.add(time);
        System.out.println(
            String.format("%s\tDC_ID:%s\tWO_ID:%s\tTIME:%s\tSEQ:%s", a, dcid, woid, time, seq));
      }
    }
    System.out.println("--------------------------------------------------");
    tmp.forEach((k, v) -> {
      v.stream().sorted()
          .forEach(seq -> System.out.println(String.format("TIME:%s\tSEQ:%s", k, seq)));
    });

    timestamps.forEach(t -> System.out.println(String.format("TIMESTAMP:%s", t)));

    es.shutdown();
    assertEquals(set.size(), size);
    System.out.println("FINISHED: " + set.size());
  }
}
