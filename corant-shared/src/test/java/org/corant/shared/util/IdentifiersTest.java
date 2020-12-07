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

import static org.corant.shared.util.Lists.listOf;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Identifiers.GeneralSnowflakeUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午7:49:51
 *
 */
public class IdentifiersTest extends TestCase {

  public static void main(String... k) {
    System.out.println(Long.class.isAssignableFrom(Long.TYPE));
    System.out.println(new GeneralSnowflakeUUIDGenerator(ChronoUnit.SECONDS, 60L,
        listOf(Pair.of(8L, 255L), Pair.of(8L, 123L)), 16L).description());
    System.out.println(new SnowflakeIpv4HostUUIDGenerator(16L).getExpirationTime());
  }

  @Test
  public void test() throws InterruptedException {
    int workers = 4, times = 65536 * 2, size = workers * times;
    GeneralSnowflakeUUIDGenerator[] generators = new GeneralSnowflakeUUIDGenerator[workers];
    final long[][] arr = new long[workers][times];
    ExecutorService es = Executors.newFixedThreadPool(workers);
    final CountDownLatch latch = new CountDownLatch(workers);
    for (int worker = 0; worker < workers; worker++) {
      final int workerId = worker;
      es.submit(() -> {
        // generators[workerId] = new SnowflakeBufferUUIDGenerator(workerId, true);
        // generators[workerId] = new SnowflakeUUIDGenerator(workerId % 3, workerId);
        long h1 = workerId % 3 == 0 ? 0L : 255L;
        generators[workerId] = new GeneralSnowflakeUUIDGenerator(ChronoUnit.SECONDS, 60,
            listOf(Pair.of(8L, h1), Pair.of(8L, (long) workerId)), 16L);
        for (int i = 0; i < times; i++) {
          arr[workerId][i] =
              generators[workerId].generate(() -> System.currentTimeMillis() / 1000 + 1);
        }
        latch.countDown();
      });
    }
    latch.await();
    Set<Long> set = new HashSet<>();
    Set<Long> timestamps = new HashSet<>();
    Map<Long, List<Long>> tmp = new LinkedHashMap<>();
    for (int workerId = 0; workerId < arr.length; workerId++) {
      for (long a : arr[workerId]) {
        set.add(a);
        long time = generators[workerId].parseGeneratedInstant(a).toEpochMilli();
        long dcid = generators[workerId].parseGeneratedWorkerId(a, 0);
        long woid = generators[workerId].parseGeneratedWorkerId(a, 1);
        long seq = generators[workerId].parseGeneratedSequence(a);
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
