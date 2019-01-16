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
package org.corant.suites.jpa.shared.metadata;

import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.split;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.corant.kernel.util.ConfigUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.JpaConfig;
import org.corant.suites.jpa.shared.JpaUtils;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午4:06:33
 *
 */
public class PersistenceConfigParser {

  private static Logger logger = Logger.getLogger(PersistenceConfigParser.class.getName());

  public static Map<String, PersistenceUnitInfoMetaData> parse(Config config) {
    Map<String, PersistenceUnitInfoMetaData> map = new HashMap<>();
    Map<String, List<String>> cfgNmes =
        ConfigUtils.getGroupConfigNames(config, JpaConfig.PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      doParse(config, k, v, map);
      logger.info(() -> String.format("Parsed persistence unit %s from config file.", k));
    });
    return map;
  }

  protected static void doParse(Config config, String name, List<String> cfgNmes,
      Map<String, PersistenceUnitInfoMetaData> map) {
    PersistenceUnitInfoMetaData puimd = new PersistenceUnitInfoMetaData(name);
    final String proPrefix = JpaConfig.PREFIX + name + JpaConfig.DOT_PUN_PRO;
    final int proPrefixLen = proPrefix.length();
    Set<String> proCfgNmes = new HashSet<>();
    cfgNmes.forEach(pn -> {
      if (pn.endsWith(JpaConfig.DOT_PUN_TRANS_TYP)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> puimd
            .setPersistenceUnitTransactionType(PersistenceUnitTransactionType.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_NON_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setNonJtaDataSourceName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setJtaDataSourceName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_PROVIDER)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setPersistenceProviderClassName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_CLS)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> asStream(split(s, ",")).forEach(puimd::addManagedClassName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_MAP_FILE)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> asStream(split(s, ",")).forEach(puimd::addMappingFileName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JAR_FILE)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> asStream(split(s, ","))
            .map(PersistenceConfigParser::toUrl).forEach(puimd::addJarFileUrl));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_EX_UL_CLS)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(puimd::setExcludeUnlistedClasses);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_VAL_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> puimd.setValidationMode(ValidationMode.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_SHARE_CACHE_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> puimd.setSharedCacheMode(SharedCacheMode.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_CLS_PKG)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> JpaUtils.getPersistenceClasses(s)
            .stream().map(Class::getName).forEach(puimd::addManagedClassName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_MAP_FILE_PATH)) {
        JpaUtils.getPersistenceMappingFiles(
            split(config.getOptionalValue(pn, String.class).orElse(JpaConfig.DFLT_ORM_XML_LOCATION),
                ";", true, true))
            .forEach(puimd::addMappingFileName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JAR_FILE)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> puimd.addJarFileUrl(toUrl(s)));
      } else if (pn.startsWith(proPrefix) && pn.length() > proPrefixLen) {
        // handle properties
        proCfgNmes.add(pn);
      }
    });
    doParseProperties(config, proPrefix, proCfgNmes, puimd);
    map.put(name, puimd);
  }

  private static void doParseProperties(Config config, String proPrefix, Set<String> proCfgNmes,
      PersistenceUnitInfoMetaData metaData) {
    if (!proCfgNmes.isEmpty()) {
      int len = proPrefix.length() + 1;
      for (String cfgNme : proCfgNmes) {
        config.getOptionalValue(cfgNme, String.class).ifPresent(s -> {
          String proName = cfgNme.substring(len);
          metaData.putPropertity(proName, s);
        });
      }
    }
  }

  private static URL toUrl(String urlstr) {
    try {
      return new URL(urlstr);
    } catch (MalformedURLException e) {
      throw new CorantRuntimeException(e);
    }
  }
}
