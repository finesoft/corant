/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Lists.append;
import static org.corant.shared.util.Lists.removeIf;
import static org.junit.Assert.assertArrayEquals;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午5:10:41
 *
 */
public class ListsTest extends TestCase {

  public static void main(String... obj) {
    SnowflakeIpv4L2sUUIDGenerator _16384 = new SnowflakeIpv4L2sUUIDGenerator(16384);
    for (int i = 0; i < 10000; i++) {
      if (i % 2000 == 0) {
        Threads.tryThreadSleep(2000L);
      }
      System.out.println(_16384.generate(Instant.now()::getEpochSecond));
    }
  }

  @Test
  public void testArrayAppend() {
    String[] array = new String[] {"a", "b", "c"};
    String[] appendArray = new String[] {"a", "b", "c", "d"};
    assertArrayEquals(append(array, "d"), appendArray);
  }

  @Test
  public void testArrayRemove() {
    String[] array = new String[] {"a"};
    String[] removedArray = Strings.EMPTY_ARRAY;
    assertArrayEquals(removeIf(array, x -> x.equals("a")), removedArray);
    assertArrayEquals(removeIf(array, x -> x.equals("x")), array);
  }

  public static class SnowflakeIpv4L2sUUIDGenerator {

    public static final long WORKER_ID_BITS = 16;// Supports 1024 workers
    public static final long MAX_WORKER_ID = -1L ^ -1L << WORKER_ID_BITS;
    public static final long SEQUENCE_BITS = 15L;// Supports 4096 serial numbers very millisecond
    public static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    public static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    public static final long SEQUENCE_MASK = -1L ^ -1L << SEQUENCE_BITS;
    public static final long FORCE_EXPEL_CACHE_PERIOD = 10 * 60; // Use for time buffer
    public static final long TIME_EPOCH =
        Instant.ofEpochMilli(Identifiers.TIME_EPOCH_MILLS).getEpochSecond();

    private final long workerId;
    private final long workerSegm;
    private final boolean useTimeBuffer;

    private volatile long lastTimestamp = -1L;
    private volatile long localLastTimestamp = -1L;
    private AtomicLong sequence = new AtomicLong(0L);

    public SnowflakeIpv4L2sUUIDGenerator(long workerId) {
      this(workerId, false);
    }

    public SnowflakeIpv4L2sUUIDGenerator(long workerId, boolean useTimeBuffer) {
      if (workerId < 0 || workerId > MAX_WORKER_ID) {
        throw new IllegalArgumentException(
            "Worker id is illegal: " + workerId + " [0," + MAX_WORKER_ID + "]");
      }
      this.workerId = workerId;
      workerSegm = workerId << WORKER_ID_SHIFT;
      this.useTimeBuffer = useTimeBuffer;
    }

    /**
     * 解析id获得时间戳
     *
     * @param id
     * @return
     */
    public static Instant parseGeningInstant(long id) {
      long timestamp = id >>> TIMESTAMP_LEFT_SHIFT;
      return Instant.ofEpochSecond(timestamp + TIME_EPOCH);
    }

    /**
     * 解析顺序号
     *
     * @param id
     * @return
     */
    public static long parseGeningSequence(long id) {
      long tmp = id << 64 - TIMESTAMP_LEFT_SHIFT + WORKER_ID_BITS;
      tmp >>>= 64 - SEQUENCE_BITS;
      return tmp;
    }

    /**
     * 解析id获得工作进程id
     *
     * @param id
     * @return
     */
    public static long parseGeningWorkerId(long id) {
      long tmp = id << 64 - TIMESTAMP_LEFT_SHIFT;
      tmp >>>= 64 - TIMESTAMP_LEFT_SHIFT + SEQUENCE_BITS;
      return tmp;
    }

    static Long getCurrentTimestamp(Supplier<?> timeGener) {
      return timeGener == null ? Instant.now().getEpochSecond() : (Long) timeGener.get();
    }

    static long tilSeconds(Supplier<?> timeGener, long lastTimestamp, boolean allowEq) {
      long timestamp = getCurrentTimestamp(timeGener);
      if (timestamp <= lastTimestamp) {
        while (allowEq ? timestamp < lastTimestamp : timestamp <= lastTimestamp) {
          timestamp = getCurrentTimestamp(timeGener);
        }
        return timestamp;
      } else {
        return timestamp;
      }
    }

    private static int handleSequence(AtomicLong sequence, Long mask, boolean current) {
      int seq = sequence.intValue();
      long incredSeq = sequence.incrementAndGet();
      incredSeq &= mask;
      if (sequence.get() != incredSeq) {
        System.out.println("======================================");
        sequence.set(incredSeq);
      }
      return current ? sequence.intValue() : seq;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      SnowflakeIpv4L2sUUIDGenerator other = (SnowflakeIpv4L2sUUIDGenerator) obj;
      return workerId == other.workerId;
    }

    /**
     * epochSecond generator
     */
    public Long generate(Supplier<?> timeGener) {
      if (useTimeBuffer) {
        return doGenerateWithCache(timeGener);
      } else {
        return doGenerateWithoutCache(timeGener);
      }
    }

    public long getWorkerId() {
      return workerId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (workerId ^ workerId >>> 32);
      return result;
    }

    protected synchronized Long doGenerateWithCache(Supplier<?> timeGener) {
      resetSequenceIfNecessary();
      int cursor = handleSequence(sequence, SEQUENCE_MASK, false);
      if (cursor == 0) {
        lastTimestamp = tilSeconds(timeGener, lastTimestamp, false);
        localLastTimestamp = Instant.now().getEpochSecond();
      }
      return nextId(lastTimestamp, cursor);
    }

    protected synchronized Long doGenerateWithoutCache(Supplier<?> timeGener) {
      long timestamp = tilSeconds(timeGener, lastTimestamp, true);
      if (lastTimestamp == timestamp) {
        int currentSeq = handleSequence(sequence, SEQUENCE_MASK, true);
        if (currentSeq == 0) {
          timestamp = tilSeconds(timeGener, lastTimestamp, false);
        }
      } else {
        sequence.set(0L);
      }
      lastTimestamp = timestamp;
      localLastTimestamp = Instant.now().getEpochSecond();
      return nextId(timestamp, sequence.get());
    }

    protected long nextId(long timestamp, long seq) {
      return timestamp - TIME_EPOCH << TIMESTAMP_LEFT_SHIFT | workerSegm | seq;
    }

    private void resetSequenceIfNecessary() {
      if (useTimeBuffer && localLastTimestamp != -1
          && Instant.now().getEpochSecond() - localLastTimestamp > FORCE_EXPEL_CACHE_PERIOD) {
        sequence.set(0);
      }
    }
  }

}
