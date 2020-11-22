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

import static org.corant.shared.util.Conversions.toLong;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.net.Inet4Address;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * @author bingo 上午12:30:13
 */
public class Identifiers {

  public static final long TIME_EPOCH_MILLS = 1_451_372_606_990L;

  static final Map<Integer, IdentifierGenerator> SNOWFLAKE_UUID_GENERATOR =
      new ConcurrentHashMap<>();
  static final Map<Integer, IdentifierGenerator> SNOWFLAKE_BUFFRE_UUID_GENERATOR =
      new ConcurrentHashMap<>();
  static final IdentifierGenerator TIME_UUID_GENERATOR = new TimeBasedUUIDGenerator();
  static final IdentifierGenerator JAVA_UUID_GENERATOR = new JavaUUIDGenerator();
  static final AtomicReference<SnowflakeUUIDGenerator> lastSnowflakeUUIDGenerator =
      new AtomicReference<>();
  static final AtomicReference<SnowflakeBufferUUIDGenerator> lastSnowflakeBufferUUIDGenerator =
      new AtomicReference<>();

  private Identifiers() {
    super();
  }

  public static long calMask(long bits) {
    return -1L ^ -1L << bits;
  }

  public static int handleSequence(AtomicLong sequence, Long mask, boolean current) {
    int seq = sequence.intValue();
    long incredSeq = sequence.incrementAndGet();
    incredSeq &= mask;
    if (sequence.get() != incredSeq) {
      sequence.set(incredSeq);
    }
    return current ? sequence.intValue() : seq;
  }

  public static String javaUUID() {
    return JAVA_UUID_GENERATOR.generate(null).toString();
  }

  public static long snowflakeBufferUUID(final int workerId, final boolean useTimeBuff,
      Supplier<Long> timeSupplier) {
    return (long) snowflakeBufferUUIDGenerator(workerId, useTimeBuff).generate(timeSupplier);
  }

  public static IdentifierGenerator snowflakeBufferUUIDGenerator(final int workerId,
      final boolean useTimeBuff) {
    SnowflakeBufferUUIDGenerator inst = lastSnowflakeBufferUUIDGenerator.get();
    if (inst != null && inst.workerId == workerId) {
      return inst;
    } else {
      return SNOWFLAKE_BUFFRE_UUID_GENERATOR.computeIfAbsent(workerId, k -> {
        SnowflakeBufferUUIDGenerator x = new SnowflakeBufferUUIDGenerator(k, useTimeBuff);
        lastSnowflakeBufferUUIDGenerator.set(x);
        return x;
      });
    }
  }

  public static long snowflakeUUID(int dataCenterId, int workerId, Supplier<Long> timeSupplier) {
    return (long) snowflakeUUIDGenerator(dataCenterId, workerId).generate(timeSupplier);
  }

  public static IdentifierGenerator snowflakeUUIDGenerator(int dataCenterId, int workerId) {
    SnowflakeUUIDGenerator inst = lastSnowflakeUUIDGenerator.get();
    if (inst != null && inst.dataCenterId == dataCenterId && inst.workerId == workerId) {
      return inst;
    } else {
      int key = dataCenterId << SnowflakeUUIDGenerator.DATACENTER_ID_BITS | workerId;
      return SNOWFLAKE_UUID_GENERATOR.computeIfAbsent(key, k -> {
        SnowflakeUUIDGenerator x = new SnowflakeUUIDGenerator(dataCenterId, workerId);
        lastSnowflakeUUIDGenerator.set(x);
        return x;
      });

    }
  }

  public static long tills(Supplier<?> timeGener, long lastTimestamp, boolean allowEq) {
    long timestamp = (long) timeGener.get();
    if (timestamp <= lastTimestamp) {
      while (allowEq ? timestamp < lastTimestamp : timestamp <= lastTimestamp) {
        timestamp = (long) timeGener.get();
      }
      return timestamp;
    } else {
      return timestamp;
    }
  }

  public static String timeBaseUUID(Supplier<Long> timeSupplier) {
    return (String) TIME_UUID_GENERATOR.generate(timeSupplier);
  }

  /**
   * The general snow flake UUID generator, Use time increment as a prefix, use numerical increment
   * as a suffix, support multi-segment infix, return 64-bit unsigned long UUID.
   *
   *
   * corant-shared
   *
   * @author bingo 下午8:31:09
   *
   */
  public static class GeneralSnowflakeUUIDGenerator implements IdentifierGenerator {

    protected final long workersBits;
    protected final long sequenceBits;
    protected final long timestampBits;
    protected final long timestampLeftShift;
    protected final long sequenceMask;
    protected final long cacheExpirationMills;
    protected final long epoch;

    protected final ChronoUnit unit;
    protected final int workerSize;
    protected final long[] workerIds;
    protected final long[] workerBits;
    protected final long[] workerSegms;

    protected volatile long lastTimestamp = -1L;
    protected volatile long localLastTimestamp = -1L;
    protected AtomicLong sequence = new AtomicLong(0L);

    /**
     * @param unit The first segment time unit
     * @param workers The middle segments
     * @param sequenceBits The last segment
     */
    public GeneralSnowflakeUUIDGenerator(ChronoUnit unit, List<Pair<Long, Long>> workers,
        long sequenceBits) {
      this(unit, -1, workers, sequenceBits);
    }

    /**
     *
     * @param unit The first segment time unit
     * @param cacheExpiration less then 1 means not use cache
     * @param workers The middle segments
     * @param sequenceBits The last segment
     */
    public GeneralSnowflakeUUIDGenerator(ChronoUnit unit, long cacheExpiration,
        List<Pair<Long, Long>> workers, long sequenceBits) {
      if (isEmpty(workers) || workers.stream().anyMatch(w -> w.getLeft() < 0 || w.getRight() < 0)
          || sequenceBits < 0) {
        throw new IllegalArgumentException(
            "The workers id bits or sequence bits error, Both parameters must be greater than zero");
      }
      workersBits = workers.stream().map(Pair::getLeft).reduce(Long::sum).get();
      this.sequenceBits = sequenceBits;
      if (workersBits + sequenceBits > 62) {
        throw new IllegalArgumentException(
            "The workers bits and sequence bits can not greater then 62");
      }
      timestampLeftShift = sequenceBits + workersBits;
      timestampBits = 64 - timestampLeftShift;
      sequenceMask = calMask(sequenceBits);
      workerSize = workers.size();
      int i = workerSize;
      workerBits = new long[i];
      workerIds = new long[i];
      workerSegms = new long[i];
      long workerShift = this.sequenceBits;
      while (i-- > 0) {
        Pair<Long, Long> worker = workers.get(i);
        workerBits[i] = worker.getLeft();
        workerIds[i] = worker.getRight();
        long maxWorkerId = calMask(workerIds[i]);
        if (workerIds[i] > maxWorkerId) {
          throw new IllegalArgumentException(
              "Worker id is illegal: " + workerIds[i] + " [0," + maxWorkerId + "]");
        }
        workerSegms[i] = workerIds[i] << workerShift;
        workerShift += workerBits[i];
      }

      if (unit != ChronoUnit.MILLIS && unit != ChronoUnit.SECONDS) {
        throw new IllegalArgumentException("Only supports second/millis");
      }
      this.unit = defaultObject(unit, ChronoUnit.MILLIS);
      cacheExpirationMills =
          cacheExpiration > 0 ? Duration.of(cacheExpiration, unit).toMillis() : -1;
      epoch = unit == ChronoUnit.SECONDS
          ? Instant.ofEpochMilli(Identifiers.TIME_EPOCH_MILLS).getEpochSecond()
          : Identifiers.TIME_EPOCH_MILLS;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      GeneralSnowflakeUUIDGenerator other = (GeneralSnowflakeUUIDGenerator) obj;
      if (cacheExpirationMills != other.cacheExpirationMills) {
        return false;
      }
      if (sequenceBits != other.sequenceBits) {
        return false;
      }
      if (unit != other.unit) {
        return false;
      }
      if (!Arrays.equals(workerIds, other.workerIds)) {
        return false;
      }
      if (!Arrays.equals(workerSegms, other.workerSegms)) {
        return false;
      }
      if (workersBits != other.workersBits) {
        return false;
      }
      return true;
    }

    @Override
    public Long generate(Supplier<?> timeGener) {
      if (cacheExpirationMills > 0) {
        return doGenerateWithCache(timeGener);
      } else {
        return doGenerateWithoutCache(timeGener);
      }
    }

    public Instant getDeathTime() {
      return unit == ChronoUnit.SECONDS
          ? Instant.ofEpochSecond((Long.MAX_VALUE >>> timestampLeftShift) + epoch)
          : Instant.ofEpochMilli((Long.MAX_VALUE >>> timestampLeftShift) + epoch);
    }

    public List<Pair<Long, Long>> getWorkers() {
      List<Pair<Long, Long>> workers = new ArrayList<>();
      for (int i = 0; i < workerSize; i++) {
        workers.add(Pair.of(workerBits[i], workerIds[i]));
      }
      return workers;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (cacheExpirationMills ^ cacheExpirationMills >>> 32);
      result = prime * result + (int) (sequenceBits ^ sequenceBits >>> 32);
      result = prime * result + (unit == null ? 0 : unit.hashCode());
      result = prime * result + Arrays.hashCode(workerIds);
      result = prime * result + Arrays.hashCode(workerSegms);
      result = prime * result + (int) (workersBits ^ workersBits >>> 32);
      return result;
    }

    public Instant parseGeningInstant(long id) {
      long timestamp = id >>> timestampLeftShift;
      return unit == ChronoUnit.SECONDS ? Instant.ofEpochSecond(timestamp + epoch)
          : Instant.ofEpochMilli(timestamp + epoch);
    }

    public long parseGeningSequence(long id) {
      long tmp = id << 64 - timestampLeftShift + workersBits;
      tmp >>>= 64 - sequenceBits;
      return tmp;
    }

    public long parseGeningWorkerId(long id, int index) {
      long ls = timestampBits;
      long rs = sequenceBits;
      for (int i = 0; i < workerSize; i++) {
        if (i < index) {
          ls += workerBits[i];
        } else if (i > index) {
          rs += workerBits[i];
        }
      }
      long tmp = id << ls;
      tmp >>>= ls + rs;
      return tmp;
    }

    protected synchronized Long doGenerateWithCache(Supplier<?> timeGener) {
      resetSequenceIfNecessary();
      int cursor = Identifiers.handleSequence(sequence, sequenceMask, false);
      if (cursor == 0) {
        lastTimestamp = Identifiers.tills(timeGener, lastTimestamp, false);
        localLastTimestamp = System.currentTimeMillis();
      }
      return nextId(lastTimestamp, cursor);
    }

    protected synchronized Long doGenerateWithoutCache(Supplier<?> timeGener) {
      long timestamp = Identifiers.tills(timeGener, lastTimestamp, true);
      if (lastTimestamp == timestamp) {
        int currentSeq = Identifiers.handleSequence(sequence, sequenceMask, true);
        if (currentSeq == 0) {
          timestamp = Identifiers.tills(timeGener, lastTimestamp, false);
        }
      } else {
        sequence.set(0L);
      }
      lastTimestamp = timestamp;
      localLastTimestamp = System.currentTimeMillis();
      return nextId(timestamp, sequence.get());
    }

    protected long nextId(long timestamp, long seq) {
      long next = timestamp - epoch << timestampLeftShift;
      for (long workerSegm : workerSegms) {
        next |= workerSegm;
      }
      return next | seq;
    }

    private void resetSequenceIfNecessary() {
      if (cacheExpirationMills > 0 && localLastTimestamp != -1
          && System.currentTimeMillis() - localLastTimestamp > cacheExpirationMills) {
        sequence.set(0);
      }
    }
  }

  /**
   * The corant-shared
   *
   * @author bingo 下午11:20:37
   *
   */
  public interface IdentifierGenerator {
    Serializable generate(Supplier<?> suppler);
  }

  /**
   * UUID generator use {@link java.util.UUID.randomUUID()}
   *
   *
   * corant-shared
   *
   * @author bingo 下午11:20:53
   *
   */
  public static class JavaUUIDGenerator implements IdentifierGenerator {
    @Override
    public Serializable generate(Supplier<?> suppler) {
      return UUID.randomUUID().toString();
    }
  }

  /**
   * The simple snow flake buffered UUID generator.
   *
   * <pre>
   * Supports 10-bit work processes, 12-bit serial numbers, 42-bit time stamps,
   * and milliseconds as the time difference unit;
   * the overall coding is as follows:
   * <b>[1...42] [1...10] [1...12]</b>
   * The first segment is the time (millisecond) step,
   * the second segment is the work process,
   * and the third segment is the serial number.
   * </pre>
   *
   *
   * corant-shared
   *
   * @author bingo 下午11:22:34
   *
   */
  public static class SnowflakeBufferUUIDGenerator extends GeneralSnowflakeUUIDGenerator {

    public static final long WORKER_ID_BITS = 10;// Supports 1024 workers
    public static final long SEQUENCE_BITS = 12L;// Supports 4096 serial numbers very millisecond
    public static final long FORCE_EXPEL_CACHE_PERIOD = 10 * 60 * 1000L; // Use for time buffer
    private final long workerId;

    public SnowflakeBufferUUIDGenerator(long workerId) {
      this(workerId, true);
    }

    public SnowflakeBufferUUIDGenerator(long workerId, boolean useTimeBuffer) {
      super(ChronoUnit.MILLIS, useTimeBuffer ? FORCE_EXPEL_CACHE_PERIOD : -1,
          listOf(Pair.of(WORKER_ID_BITS, workerId)), SEQUENCE_BITS);
      this.workerId = workerId;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SnowflakeBufferUUIDGenerator other = (SnowflakeBufferUUIDGenerator) obj;
      if (workerId != other.workerId) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (int) (workerId ^ workerId >>> 32);
      return result;
    }

  }

  /**
   * Use the host part of IPV4 as the work segment, the time increments in seconds as the prefix,
   * and the suffix is a 16-digit increment; it is usually used for K8s cluster UUID generation.
   * Note that since it is incremented by second, {@code timeGener} must also be seconds from the
   * epoch of 1970-01-01T00:00:00Z.
   *
   * corant-shared
   *
   * @author bingo 下午8:49:33
   *
   */
  public static class SnowflakeIpv4HostUUIDGenerator extends GeneralSnowflakeUUIDGenerator {

    protected final Inet4Address ip;

    public SnowflakeIpv4HostUUIDGenerator(Inet4Address ip) {
      this(ip, -1);
    }

    public SnowflakeIpv4HostUUIDGenerator(Inet4Address ip, long cacheExpiration) {
      super(ChronoUnit.SECONDS, cacheExpiration,
          listOf(Pair.of(8L, toLong(ip.getAddress()[2] & 0xff)),
              Pair.of(8L, toLong(ip.getAddress()[3] & 0xff))),
          16);
      this.ip = ip;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SnowflakeIpv4HostUUIDGenerator other = (SnowflakeIpv4HostUUIDGenerator) obj;
      if (ip == null) {
        if (other.ip != null) {
          return false;
        }
      } else if (!ip.equals(other.ip)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (ip == null ? 0 : ip.hashCode());
      return result;
    }

  }

  /**
   * <pre>
   * 根据Twitter的算法实现的id生成器，同一个应用实例内只能单例使用。 当前的实现假设，
   * 所有进程依赖同一个数据库实例，因此依赖数据库授时；如果不是则需另外实现。 <br/>
   * 1~41 为当前时间至1451372606990L（2015-12-29 15:15:???）的时间差（毫秒） <br/>
   * 42~46为子系统或数据中心编号，2^5即从0~31 <br/>
   * 47~51为进程编号，2^5即从0~31 <br/>
   * 52~63为每个时间毫秒内的顺序号，2^12即从0~4095号，共12位 <br/>
   * 整体表现：同一毫秒内允许1024个进程进行id生成，每个进程可生成4096个顺序id <br/>
   * 注意不可用日期为 ：<b>2085-09-04T06:51:02.541+08:00[Asia/Shanghai]</b>
   * 如果有人在那天遇到该问题，如果long还是只有64位的话，请换掉它！
   * </pre>
   *
   * @author bingo 2016年3月9日
   * @since
   */
  public static class SnowflakeUUIDGenerator extends GeneralSnowflakeUUIDGenerator {

    public static final long WORKER_ID_BITS = 5L; // Supports 32 workers
    public static final long DATACENTER_ID_BITS = 5L;// Supports 32 data centers
    public static final long SEQUENCE_BITS = 12L;// Supports 4096 serial numbers very millisecond

    private final long dataCenterId;
    private final long workerId;

    public SnowflakeUUIDGenerator(long dataCenterId, long workerId) {
      super(ChronoUnit.MILLIS,
          listOf(Pair.of(DATACENTER_ID_BITS, dataCenterId), Pair.of(WORKER_ID_BITS, workerId)),
          SEQUENCE_BITS);
      this.dataCenterId = dataCenterId;
      this.workerId = workerId;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SnowflakeUUIDGenerator other = (SnowflakeUUIDGenerator) obj;
      if (dataCenterId != other.dataCenterId) {
        return false;
      }
      if (workerId != other.workerId) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (int) (dataCenterId ^ dataCenterId >>> 32);
      result = prime * result + (int) (workerId ^ workerId >>> 32);
      return result;
    }

  }

  public static class TimeBasedUUIDGenerator implements IdentifierGenerator {

    static final SecureRandom SEC_RDM_INST = new SecureRandom();

    private static final byte[] SECURE_MUNGED_ADDRESS =
        MacAddrs.getSecureMungedAddress(SEC_RDM_INST);

    static {
      assert SECURE_MUNGED_ADDRESS.length == 6;
    }

    private final AtomicInteger sequence = new AtomicInteger(SEC_RDM_INST.nextInt());

    private long lastTimestamp;

    private static void putLong(byte[] array, long l, int pos, int numberOfLongBytes) {
      for (int i = 0; i < numberOfLongBytes; ++i) {
        array[pos + numberOfLongBytes - i - 1] = (byte) (l >>> (i << 3));
      }
    }

    @Override
    public String generate(Supplier<?> suppler) {
      final int sequenceId = sequence.incrementAndGet() & 0xffffff;
      long timestamp = (Long) suppler.get();

      synchronized (this) {
        timestamp = Math.max(lastTimestamp, timestamp);

        if (sequenceId == 0) {
          timestamp++;
        }

        lastTimestamp = timestamp;
      }

      final byte[] uuidBytes = new byte[15];

      putLong(uuidBytes, timestamp, 0, 6);

      System.arraycopy(SECURE_MUNGED_ADDRESS, 0, uuidBytes, 6, SECURE_MUNGED_ADDRESS.length);

      putLong(uuidBytes, sequenceId, 12, 3);

      assert 9 + SECURE_MUNGED_ADDRESS.length == uuidBytes.length;

      return Base64.getUrlEncoder().encodeToString(uuidBytes);
    }
  }
}
