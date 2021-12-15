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
package org.corant.modules.jpa.shared.metadata;

import static org.corant.config.CorantConfigResolver.getGroupConfigKeys;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.corant.modules.jpa.shared.JPAConfig;
import org.corant.modules.jpa.shared.JPAUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午4:06:33
 *
 */
public class PersistencePropertiesParser {

  private static Logger logger = Logger.getLogger(PersistencePropertiesParser.class.getName());

  public static Set<PersistenceUnitInfoMetaData> parse(Config config) {
    Set<PersistenceUnitInfoMetaData> cfgs = new HashSet<>();
    Set<String> dfltCfgKeys = JPAConfig.defaultPropertyNames(config);
    String dfltPuNme = config
        .getOptionalValue(JPAConfig.JC_PREFIX + JPAConfig.JC_PU_NME.substring(1), String.class)
        .orElse(null);
    doParse(config, dfltPuNme, dfltCfgKeys, cfgs);// defaults
    Map<String, List<String>> namedCfgKeys = getGroupConfigKeys(config,
        s -> defaultString(s).startsWith(JPAConfig.JC_PREFIX) && !dfltCfgKeys.contains(s), 2);
    namedCfgKeys.forEach((k, v) -> {
      doParse(config, k, v, cfgs);
      logger.fine(() -> String.format("Parsed persistence unit [%s] from config file.", k));
    });
    return cfgs;
  }

  protected static void doParse(Config config, String key, Collection<String> cfgNmes,
      Set<PersistenceUnitInfoMetaData> cfgs) {
    PersistenceUnitInfoMetaData puimd = new PersistenceUnitInfoMetaData(key);
    final String proPrefix = isNotBlank(puimd.getPersistenceUnitName())
        ? JPAConfig.JC_PREFIX + puimd.getPersistenceUnitName() + JPAConfig.JC_PRO
        : JPAConfig.JC_PREFIX + JPAConfig.JC_PRO.substring(1);
    final int proPrefixLen = proPrefix.length();
    Set<String> proCfgNmes = new HashSet<>();
    cfgNmes.forEach(pn -> {
      if (pn.startsWith(proPrefix) && pn.length() > proPrefixLen) {
        // handle properties
        proCfgNmes.add(pn);
      } else if (pn.endsWith(JPAConfig.JC_TRANS_TYP)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> puimd
            .setPersistenceUnitTransactionType(PersistenceUnitTransactionType.valueOf(s)));
      } else if (pn.endsWith(JPAConfig.JC_NON_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setNonJtaDataSourceName);
      } else if (pn.endsWith(JPAConfig.JC_JTA_DS)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setJtaDataSourceName);
      } else if (pn.endsWith(JPAConfig.JC_PROVIDER)) {
        config.getOptionalValue(pn, String.class).ifPresent(puimd::setPersistenceProviderClassName);
      } else if (pn.endsWith(JPAConfig.JC_CLS)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> streamOf(split(s, ",")).forEach(puimd::addManagedClassName));
      } else if (pn.endsWith(JPAConfig.JC_MAP_FILE)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> streamOf(split(s, ",")).forEach(puimd::addMappingFileName));
      } else if (pn.endsWith(JPAConfig.JC_JAR_FILE)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> streamOf(split(s, ","))
            .map(PersistencePropertiesParser::toUrl).forEach(puimd::addJarFileUrl));
      } else if (pn.endsWith(JPAConfig.JC_EX_UL_CLS)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(puimd::setExcludeUnlistedClasses);
      } else if (pn.endsWith(JPAConfig.JC_VAL_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> puimd.setValidationMode(ValidationMode.valueOf(s)));
      } else if (pn.endsWith(JPAConfig.JC_ENABLE)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(puimd::setEnable);
      } else if (pn.endsWith(JPAConfig.JC_SHARE_CACHE_MOD)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> puimd.setSharedCacheMode(SharedCacheMode.valueOf(s)));
      } else if (pn.endsWith(JPAConfig.JC_CLS_PKG)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> {
          streamOf(split(s, ",", true, true)).forEach(p -> {
            JPAUtils.getPersistenceClasses(p).stream().map(Class::getName)
                .forEach(puimd::addManagedClassName);
          });
        });
      } else if (pn.endsWith(JPAConfig.JC_MAP_FILE_PATH)) {
        JPAUtils.getPersistenceMappingFiles(
            split(config.getOptionalValue(pn, String.class).orElse(JPAConfig.DFLT_ORM_XML_LOCATION),
                ",", true, true))
            .forEach(puimd::addMappingFileName);
      }
    });

    if (isEmpty(puimd.getManagedClassNames()) && isEmpty(puimd.getJarFileUrls())) {
      if (isNotEmpty(cfgs)) {
        logger.warning(() -> String.format(
            "Can not find any managed classes or jars for persistence unit %s, the persistence unit will be ignored!",
            puimd.getPersistenceUnitName()));
      }
    } else {
      if (isBlank(puimd.getPersistenceProviderClassName())) {
        puimd.resolvePersistenceProvider();
        logger.warning(() -> String.format(
            "Can't find configured persistence provider class for persistence unit %s, use the runtime persistence provider %s.",
            puimd.getPersistenceUnitName(), puimd.getPersistenceProviderClassName()));
      }
      doParseProperties(config, proPrefix, proCfgNmes, puimd);
      shouldBeTrue(cfgs.add(puimd), "The jpa configuration error persistence unit name %s dup!",
          puimd.getPersistenceUnitName());
    }
  }

  private static void doParseProperties(Config config, String proPrefix, Set<String> proCfgNmes,
      PersistenceUnitInfoMetaData metaData) {
    if (!proCfgNmes.isEmpty()) {
      int len = proPrefix.length() + 1;
      for (String cfgNme : proCfgNmes) {
        config.getOptionalValue(cfgNme, String.class).ifPresent(s -> {
          String proName = cfgNme.substring(len);
          if (JPAConfig.BIND_JNDI.equals(proName)) {
            metaData.setBindToJndi(toBoolean(s));
          } else {
            metaData.putPropertity(proName, s);
          }
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
