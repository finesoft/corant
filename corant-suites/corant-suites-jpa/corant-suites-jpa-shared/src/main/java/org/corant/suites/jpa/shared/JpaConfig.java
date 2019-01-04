/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared;

import static org.corant.shared.util.ObjectUtils.shouldBeFalse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.corant.shared.util.ClassPaths;
import org.corant.suites.jpa.shared.metadata.PersistenceConfigParser;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceXmlParser;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:32:07
 *
 */
public class JpaConfig {

  public static final String PREFIX = "jpa.";
  public static final String DFLT_PU_XML_LOCATION = "META-INF/persistence.xml";
  public static final String DFLT_ORM_XML_LOCATION = "META-INF/orm.xml";
  public static final String PUN_TAG = "persistence-unit";
  public static final String PUN_NME = "name";
  public static final String PUN_TRANS_TYP = "transaction-type";
  public static final String PUN_NON_JTA_DS = "non-jta-data-source";
  public static final String PUN_JTA_DS = "jta-data-source";
  public static final String PUN_PROVIDER = "provider";
  public static final String PUN_CLS = "class";
  public static final String PUN_MAP_FILE = "mapping-file";
  public static final String PUN_JAR_FILE = "jar-file";
  public static final String PUN_EX_UL_CLS = "exclude-unlisted-classes";
  public static final String PUN_VAL_MOD = "validation-mode";
  public static final String PUN_SHARE_CACHE_MOD = "shared-cache-mode";
  public static final String PUN_PROS = "properties";
  public static final String PUN_PRO = "property";
  public static final String PUN_PRO_NME = "name";
  public static final String PUN_PRO_VAL = "value";

  public static final String DOT_PUN_NME = "." + PUN_NME;
  public static final String DOT_PUN_TRANS_TYP = "." + PUN_TRANS_TYP;
  public static final String DOT_PUN_NON_JTA_DS = "." + PUN_NON_JTA_DS;
  public static final String DOT_PUN_JTA_DS = "." + PUN_JTA_DS;
  public static final String DOT_PUN_PROVIDER = "." + PUN_PROVIDER;
  public static final String DOT_PUN_CLS = "." + PUN_CLS;
  public static final String DOT_PUN_PKG = DOT_PUN_CLS + "-package";
  public static final String DOT_PUN_MAP_FILE = "." + PUN_MAP_FILE;
  public static final String DOT_PUN_MAP_FILE_REGEX = DOT_PUN_MAP_FILE + "-regex";
  public static final String DOT_PUN_JAR_FILE = "." + PUN_JAR_FILE;
  public static final String DOT_PUN_EX_UL_CLS = "." + PUN_EX_UL_CLS;
  public static final String DOT_PUN_VAL_MOD = "." + PUN_VAL_MOD;
  public static final String DOT_PUN_SHARE_CACHE_MOD = "." + PUN_SHARE_CACHE_MOD;
  public static final String DOT_PUN_PROS = "." + PUN_PROS;
  public static final String DOT_PUN_PRO = "." + "property";
  public static final String DOT_PUN_PRO_NME = "." + PUN_PRO_NME;
  public static final String DOT_PUN_PRO_VAL = "." + PUN_PRO_VAL;

  protected static final Logger logger = Logger.getLogger(JpaConfig.class.getName());
  private final Map<String, PersistenceUnitInfoMetaData> metaDatas = new HashMap<>();

  public static JpaConfig from(Config config) {
    JpaConfig cfg = new JpaConfig();
    Map<String, PersistenceUnitInfoMetaData> fromCfgPums = generateFromConfig(config);
    cfg.metaDatas.putAll(fromCfgPums);
    Map<String, PersistenceUnitInfoMetaData> fromXmlPums = generateFromXml();
    if (!fromXmlPums.isEmpty()) {
      shouldBeFalse(fromXmlPums.keySet().stream().map(cfg.metaDatas::containsKey)
          .reduce(Boolean::logicalOr).orElse(Boolean.FALSE), "The persistence unit name dup!");
    }
    cfg.metaDatas.putAll(fromXmlPums);
    logger
        .info(() -> String.format("Find persistence unit metadata from config file %s and %s %s",
            String.join(",", fromCfgPums.keySet()), DFLT_PU_XML_LOCATION,
            String.join(",", fromXmlPums.keySet())));
    return cfg;
  }

  private static Map<String, PersistenceUnitInfoMetaData> generateFromConfig(Config config) {
    return PersistenceConfigParser.parse(config);
  }

  private static Map<String, PersistenceUnitInfoMetaData> generateFromXml() {
    Map<String, PersistenceUnitInfoMetaData> map = new LinkedHashMap<>();
    try {
      ClassPaths.from(DFLT_PU_XML_LOCATION).getResources().map(r -> r.getUrl())
          .map(PersistenceXmlParser::parse).forEach(m -> {
            map.putAll(m);
          });
    } catch (IOException e) {
      logger.warning(() -> String.format("Parse persistence unit meta data from %s error %s",
          DFLT_PU_XML_LOCATION, e.getMessage()));
    }
    return map;
  }

  public Map<String, PersistenceUnitInfoMetaData> getMetaDatas() {
    return Collections.unmodifiableMap(metaDatas);
  }

}
