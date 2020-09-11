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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Strings.segment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.corant.shared.util.Chars;
import org.corant.shared.util.Identifiers;
import org.corant.shared.util.Identifiers.IdentifierGenerator;
import org.corant.shared.util.Identifiers.SnowflakeBufferUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeUUIDGenerator;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 18:36:36
 *
 */
public class SnowflakeIdGenerators {

  static final Map<String, IdentifierGenerator> namedGenerators = new ConcurrentHashMap<>();
  static final Map<Integer, IdentifierGenerator> idedGenerators = new ConcurrentHashMap<>();

  public static IdentifierGenerator get(final int workId) {
    return get(workId, true);
  }

  public static IdentifierGenerator get(final int workId, final boolean useTimeBuffer) {
    shouldBeTrue(workId >= 0 && workId <= SnowflakeBufferUUIDGenerator.MAX_WORKER_ID,
        "Worker id must >=0 and <=%s.", SnowflakeBufferUUIDGenerator.MAX_WORKER_ID);
    return new Identifiers.SnowflakeBufferUUIDGenerator(workId, useTimeBuffer);
  }

  public static IdentifierGenerator get(final int datacenterId, final int workId) {
    shouldBeTrue(datacenterId >= 0 && datacenterId <= SnowflakeUUIDGenerator.MAX_DATACENTER_ID,
        "Datacenter id must >=0 and <=%s.", SnowflakeUUIDGenerator.MAX_DATACENTER_ID);
    shouldBeTrue(workId >= 0 && workId <= SnowflakeUUIDGenerator.MAX_WORKER_ID,
        "Worker id must >=0 and <=%s.", SnowflakeUUIDGenerator.MAX_WORKER_ID);
    return idedGenerators.computeIfAbsent(datacenterId,
        d -> new Identifiers.SnowflakeUUIDGenerator(d, workId));
  }

  public static IdentifierGenerator get(final String datacenterAndWorkId,
      final boolean useTimeBuffer) {
    shouldNotBlank(datacenterAndWorkId);
    return namedGenerators.computeIfAbsent(datacenterAndWorkId, dnw -> {
      String[] dws = segment(dnw, Chars::isAsciiNumeric);
      shouldBeTrue(dws.length > 1, IllegalArgumentException::new);
      shouldBeTrue(dws[dws.length - 1].chars().allMatch(Chars::isAsciiNumeric));
      Integer workId = shouldNotNull(toInteger(dws[dws.length - 1]));
      return get(workId, useTimeBuffer);
    });
  }

  public static IdentifierGenerator get(final String datacenterName, final int workId,
      final boolean useTimeBuffer) {
    return namedGenerators.computeIfAbsent(shouldNotBlank(datacenterName),
        d -> get(workId, useTimeBuffer));
  }
}
