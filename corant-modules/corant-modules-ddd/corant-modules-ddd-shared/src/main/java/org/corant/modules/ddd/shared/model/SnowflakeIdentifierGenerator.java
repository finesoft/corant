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
package org.corant.modules.ddd.shared.model;

import static org.corant.context.Instances.findAnyway;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import org.corant.config.Configs;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Identifiers.GeneralSnowflakeUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeD5W5S12UUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeW10S12UUIDGenerator;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午7:59:56
 *
 */
@ApplicationScoped
public class SnowflakeIdentifierGenerator {

  public static final String IG_SF_WK_IP = "corant.identifier.generator.snowflake.worker-ip";
  public static final String IG_SF_WK_ID = "corant.identifier.generator.snowflake.worker-id";
  public static final String IG_SF_DC_ID = "corant.identifier.generator.snowflake.datacenter-id";
  public static final String IG_SF_DL_TM = "corant.identifier.generator.snowflake.delayed-timing";
  static Logger logger = Logger.getLogger(SnowflakeIdentifierGenerator.class.getName());

  TimeService specTimeGenerator;
  GeneralSnowflakeUUIDGenerator generator;

  public long generate(Object object) {
    return generator
        .generate(() -> specTimeGenerator.get(object, generator.getUnit() == ChronoUnit.SECONDS));
  }

  @PostConstruct
  synchronized void initialize() {
    specTimeGenerator = findAnyway(TimeService.class)
        .orElse((o, s) -> s ? System.currentTimeMillis() / 1000L + 1 : System.currentTimeMillis());
    int dataCenterId = Configs.getValue(IG_SF_DC_ID, Integer.class, -1);
    int workerId = Configs.getValue(IG_SF_WK_ID, Integer.class, -1);
    String ip = Configs.getValue(IG_SF_WK_IP, String.class);
    long delayedTiming = Configs.getValue(IG_SF_DL_TM, Long.class, 16000L);
    if (workerId >= 0) {
      if (dataCenterId >= 0) {
        generator = new SnowflakeD5W5S12UUIDGenerator(dataCenterId, workerId, delayedTiming);
      } else {
        generator = new SnowflakeW10S12UUIDGenerator(workerId, delayedTiming);
      }
    } else if (isNotBlank(ip)) {
      generator = new SnowflakeIpv4HostUUIDGenerator(ip, delayedTiming);
    } else {
      generator = new SnowflakeIpv4HostUUIDGenerator(delayedTiming);
    }
    logger.info(
        () -> String.format("Create global identifier generator %s.", generator.description()));
  }

  public interface TimeService extends Sortable {
    long get(Object entity, boolean useEpochSeconds);
  }
}
