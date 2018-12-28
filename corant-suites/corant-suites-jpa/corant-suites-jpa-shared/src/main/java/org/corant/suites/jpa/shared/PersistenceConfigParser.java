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

import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.split;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.corant.kernel.util.ConfigUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午4:06:33
 *
 */
public class PersistenceConfigParser {

  public static Map<String, PersistenceUnitMetaData> parse(Config config) {
    Map<String, PersistenceUnitMetaData> map = new HashMap<>();
    Map<String, List<String>> cfgNmes =
        ConfigUtils.getGroupConfigNames(config, JpaConfig.PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      doParse(config, k, v, map);
    });
    return map;
  }

  protected static void doParse(Config config, String name, List<String> cfgNmes,
      Map<String, PersistenceUnitMetaData> map) {
    PersistenceUnitMetaData cfg = new PersistenceUnitMetaData(name);
    final String proPrefix = JpaConfig.PREFIX + name + JpaConfig.DOT_PUN_PRO;
    final int proPrefixLen = proPrefix.length();
    Set<String> proCfgNmes = new HashSet<>();
    cfgNmes.forEach(pn -> {
      if (pn.endsWith(JpaConfig.DOT_PUN_TRANS_TYP)) {
        config.getOptionalValue(pn, String.class).ifPresent(
            s -> cfg.setPersistenceUnitTransactionType(PersistenceUnitTransactionType.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_NON_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setNonJtaDataSourceName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setJtaDataSourceName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_PROVIDER)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setPersistenceProviderClassName);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_CLS)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> asStream(split(s, ",")).forEach(cfg::addManagedClassName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_MAP_FILE)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> asStream(split(s, ",")).forEach(cfg::addMappingFileName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JAR_FILE)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> asStream(split(s, ","))
            .map(PersistenceConfigParser::toUrl).forEach(cfg::addJarFileUrl));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_EX_UL_CLS)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setExcludeUnlistedClasses);
      } else if (pn.endsWith(JpaConfig.DOT_PUN_VAL_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setValidationMode(ValidationMode.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_SHARE_CACHE_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setSharedCacheMode(SharedCacheMode.valueOf(s)));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_PKG)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> JpaUtils.getPersistenceClasses(s)
            .stream().map(Class::getName).forEach(cfg::addManagedClassName));
      } else if (pn.endsWith(JpaConfig.DOT_PUN_MAP_FILE_REGEX)) {
      } else if (pn.endsWith(JpaConfig.DOT_PUN_JAR_FILE)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.addJarFileUrl(toUrl(s)));
      } else if (pn.startsWith(proPrefix) && pn.length() > proPrefixLen) {
        // handle properties
        proCfgNmes.add(pn);
      }
    });
    doParseProperties(config, proPrefix, proCfgNmes, cfg);
    map.put(name, cfg);
  }

  private static void doParseProperties(Config config, String proPrefix, Set<String> proCfgNmes,
      PersistenceUnitMetaData metaData) {
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
