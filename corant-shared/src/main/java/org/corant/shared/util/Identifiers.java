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
import static org.corant.shared.util.Randoms.SECURITY_RANDOM;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-shared
 *
 * @author bingo 上午12:30:13
 */
public class Identifiers {

  public static final long TIME_EPOCH_MILLS = 1_451_372_606_990L;

  private Identifiers() {}

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

  public static long tills(Supplier<?> timeGener, long lastTimestamp, boolean allowEq) {
    long timestamp = toLong(timeGener.get());
    if (timestamp <= lastTimestamp) {
      while (allowEq ? timestamp < lastTimestamp : timestamp <= lastTimestamp) {
        timestamp = toLong(timeGener.get());
      }
    }
    return timestamp;
  }

  /**
   * The general snowflake UUID generator, Use time increment as a prefix, use numerical increment
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
    protected final long delayedTimingMs;
    protected final long epoch;

    protected final ChronoUnit unit;
    protected final int workerSize;
    protected final long[] workerIds;
    protected final long[] workerBits;
    protected final long[] workerSegms;

    protected volatile long lastTimestamp = -1L;
    protected volatile long localLastTimestamp = -1L;
    protected final AtomicLong sequence = new AtomicLong(0L);

    /**
     * Construct a generator without delay
     *
     * @param unit The prefix segment epoch time unit, current we only support MILLIS and SECOND
     * @param workers The infix segments, use an ordered pairs, every pair contains two values, one
     *        is the worker bits the other is the worker id
     * @param sequenceBits The last suffix segment bits
     */
    public GeneralSnowflakeUUIDGenerator(ChronoUnit unit, List<Pair<Long, Long>> workers,
        long sequenceBits) {
      this(unit, -1, workers, sequenceBits);
    }

    /**
     * Construct a generator
     *
     * @param unit The prefix segment epoch time unit, current we only support MILLIS and SECOND
     * @param delayedTimingMs less than 1 means no delay. No delay means that the time will be
     *        retrieved in each the ID is generated; delay means that the time will be compared in
     *        each the ID is generated, and the time will be re-retrieved when the serial number
     *        overflows or the time exceeds the delay; using delay will reduce the pressure of time
     *        service, but the time of the generated id will be a bit delayed.
     * @param workers The infix segments, use an ordered pairs, every pair contains two values, one
     *        is the worker bits the other is the worker id
     * @param sequenceBits The last suffix bits
     */
    public GeneralSnowflakeUUIDGenerator(ChronoUnit unit, long delayedTimingMs,
        List<Pair<Long, Long>> workers, long sequenceBits) {
      if (isEmpty(workers) || workers.stream().anyMatch(w -> w.getLeft() < 0 || w.getRight() < 0)
          || sequenceBits < 0) {
        throw new IllegalArgumentException(
            "The workers id bits or sequence bits error, both parameters must be greater than zero");
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
        long maxWorkerId = calMask(workerBits[i]);
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
      this.delayedTimingMs = delayedTimingMs > 0 ? delayedTimingMs : -1;
      epoch = unit == ChronoUnit.SECONDS
          ? Instant.ofEpochMilli(Identifiers.TIME_EPOCH_MILLS).getEpochSecond()
          : Identifiers.TIME_EPOCH_MILLS;
    }

    public String description() {
      StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
      sb.append(" increment ").append(unit.name().toLowerCase(Locale.ROOT)).append(" bits: ");
      sb.append("[1...").append(timestampBits).append("], worker bits:");
      long tmp = timestampBits;
      for (int i = 0; i < workerSize; i++) {
        sb.append(" [").append(tmp + 1).append("...").append(tmp += workerBits[i]).append("]")
            .append(" id ").append(workerIds[i]).append(", ");
      }
      sb.append("sequence bits: [").append(tmp + 1).append("...").append(64).append("], ");
      if (delayedTimingMs > 0) {
        sb.append("delayed timing ").append(delayedTimingMs).append("ms, ");
      }
      sb.append("support up to ").append(getExpirationTime());
      return sb.toString();
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
      if (delayedTimingMs != other.delayedTimingMs) {
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
      return workersBits == other.workersBits;
    }

    @Override
    public Long generate(Supplier<?> timeGener) {
      if (delayedTimingMs > 0) {
        return doGenerateWithCache(timeGener);
      } else {
        return doGenerateWithoutCache(timeGener);
      }
    }

    /**
     * Returns the expiration time of the generator, we use time increment as the prefix, and return
     * an unsigned long integer (64 bits), so there is a time point of failure.
     *
     * @return getExpirationTime
     */
    public Instant getExpirationTime() {
      return unit == ChronoUnit.SECONDS
          ? Instant.ofEpochSecond((Long.MAX_VALUE >>> timestampLeftShift) + epoch)
          : Instant.ofEpochMilli((Long.MAX_VALUE >>> timestampLeftShift) + epoch);
    }

    /**
     *
     * @return the unit
     */
    public ChronoUnit getUnit() {
      return unit;
    }

    /**
     * Returns the ordered workers pairs, every pair contains two values, one is the worker bits the
     * other is the worker id.
     *
     * @return getWorkers
     */
    public List<Pair<Long, Long>> getWorkers() {
      List<Pair<Long, Long>> workers = new ArrayList<>(workerSize);
      for (int i = 0; i < workerSize; i++) {
        workers.add(Pair.of(workerBits[i], workerIds[i]));
      }
      return workers;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (delayedTimingMs ^ delayedTimingMs >>> 32);
      result = prime * result + (int) (sequenceBits ^ sequenceBits >>> 32);
      result = prime * result + (unit == null ? 0 : unit.hashCode());
      result = prime * result + Arrays.hashCode(workerIds);
      result = prime * result + Arrays.hashCode(workerSegms);
      return prime * result + (int) (workersBits ^ workersBits >>> 32);
    }

    /**
     * Reverse analysis, returning the instant when it was generated from the incoming id parameter.
     *
     * @param id
     * @return parseGeneratedInstant
     */
    public Instant parseGeneratedInstant(long id) {
      long timestamp = id >>> timestampLeftShift;
      return unit == ChronoUnit.SECONDS ? Instant.ofEpochSecond(timestamp + epoch)
          : Instant.ofEpochMilli(timestamp + epoch);
    }

    /**
     * Reverse analysis, returning the sequence when it was generated from the incoming id
     * parameter.
     *
     * @param id
     * @return parseGeneratedSequence
     */
    public long parseGeneratedSequence(long id) {
      long tmp = id << 64 - timestampLeftShift + workersBits;
      tmp >>>= 64 - sequenceBits;
      return tmp;
    }

    /**
     * Reverse analysis, returning the worker id when it was generated from the incoming id and
     * index parameters.
     *
     * @param id
     * @param index
     * @return parseGeneratedWorkerId
     */
    public long parseGeneratedWorkerId(long id, int index) {
      long ls = timestampBits;
      long rs = sequenceBits;
      if (index >= 0) {
        for (int i = 0; i < workerSize; i++) {
          if (i < index) {
            ls += workerBits[i];
          } else if (i > index) {
            rs += workerBits[i];
          }
        }
      }
      long tmp = id << ls;
      tmp >>>= ls + rs;
      return tmp;
    }

    public long parseGeneratedWorkersId(long id) {
      return parseGeneratedWorkerId(id, -1);
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
      if (delayedTimingMs > 0 && localLastTimestamp != -1
          && System.currentTimeMillis() - localLastTimestamp > delayedTimingMs) {
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
   * UUID generator use {@link java.util.UUID#randomUUID()}
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
   * <pre>
   * 根据Twitter的算法实现的id生成器，同一个应用实例内只能单例使用。 当前的实现假设，
   * 所有进程依赖同一个数据库实例，因此依赖数据库授时；如果不是则需另外实现。
   * 1~41 为当前时间至1451372606990L（2015-12-29 15:15:???）的时间差（毫秒）
   * 42~46为子系统或数据中心编号，2^5即从0~31
   * 47~51为进程编号，2^5即从0~31
   * 52~63为每个时间毫秒内的顺序号，2^12即从0~4095号，共12位
   * 整体表现：同一毫秒内允许1024个进程进行id生成，每个进程可生成4096个顺序id
   * 注意不可用日期为 ：<b>2085-09-04T06:51:02.541+08:00[Asia/Shanghai]</b>
   * 如果有人在那天遇到该问题，如果long还是只有64位的话，请换掉它！
   * </pre>
   *
   * @author bingo 2016年3月9日
   * @since
   */
  public static class SnowflakeD5W5S12UUIDGenerator extends GeneralSnowflakeUUIDGenerator {

    public static final long WORKER_ID_BITS = 5L; // Supports 32 workers
    public static final long DATACENTER_ID_BITS = 5L;// Supports 32 data centers
    public static final long SEQUENCE_BITS = 12L;// Supports 4096 serial numbers very millisecond

    private final long dataCenterId;
    private final long workerId;

    public SnowflakeD5W5S12UUIDGenerator(long dataCenterId, long workerId) {
      this(dataCenterId, workerId, -1L);
    }

    public SnowflakeD5W5S12UUIDGenerator(long dataCenterId, long workerId, long delayedTimingMs) {
      super(ChronoUnit.MILLIS, delayedTimingMs,
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
      SnowflakeD5W5S12UUIDGenerator other = (SnowflakeD5W5S12UUIDGenerator) obj;
      if (dataCenterId != other.dataCenterId) {
        return false;
      }
      return workerId == other.workerId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (int) (dataCenterId ^ dataCenterId >>> 32);
      return prime * result + (int) (workerId ^ workerId >>> 32);
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

    public SnowflakeIpv4HostUUIDGenerator(Inet4Address ip, long delayedTimingMs) {
      super(ChronoUnit.SECONDS, delayedTimingMs,
          listOf(Pair.of(8L, toLong(ip.getAddress()[2] & 0xff)),
              Pair.of(8L, toLong(ip.getAddress()[3] & 0xff))),
          16);
      this.ip = ip;
    }

    public SnowflakeIpv4HostUUIDGenerator(long delayedTimingMs) {
      this(resolveIpAddress(null), delayedTimingMs);
    }

    public SnowflakeIpv4HostUUIDGenerator(String ip, long delayedTimingMs) {
      this(resolveIpAddress(ip), delayedTimingMs);
    }

    static Inet4Address resolveIpAddress(String ip) {
      try {
        return isNotBlank(ip) ? (Inet4Address) InetAddress.getByName(ip)
            : (Inet4Address) InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        throw new CorantRuntimeException(e);
      }
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
        return other.ip == null;
      } else {
        return ip.equals(other.ip);
      }
    }

    public Inet4Address getIp() {
      return ip;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      return prime * result + (ip == null ? 0 : ip.hashCode());
    }

  }

  /**
   * The simple snowflake buffered UUID generator.
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
  public static class SnowflakeW10S12UUIDGenerator extends GeneralSnowflakeUUIDGenerator {

    public static final long WORKER_ID_BITS = 10;// Supports 1024 workers
    public static final long SEQUENCE_BITS = 12L;// Supports 4096 serial numbers very millisecond
    private final long workerId;

    public SnowflakeW10S12UUIDGenerator(long workerId) {
      this(workerId, -1L);
    }

    public SnowflakeW10S12UUIDGenerator(long workerId, long cacheExpiration) {
      super(ChronoUnit.MILLIS, cacheExpiration, listOf(Pair.of(WORKER_ID_BITS, workerId)),
          SEQUENCE_BITS);
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
      SnowflakeW10S12UUIDGenerator other = (SnowflakeW10S12UUIDGenerator) obj;
      return workerId == other.workerId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      return prime * result + (int) (workerId ^ workerId >>> 32);
    }

  }

  public static class TimeBasedUUIDGenerator implements IdentifierGenerator {

    private static final byte[] SECURE_MUNGED_ADDRESS =
        Systems.getSecureMungedAddress(SECURITY_RANDOM);

    static {
      assert SECURE_MUNGED_ADDRESS.length == 6;
    }

    private final AtomicInteger sequence = new AtomicInteger(SECURITY_RANDOM.nextInt());

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
