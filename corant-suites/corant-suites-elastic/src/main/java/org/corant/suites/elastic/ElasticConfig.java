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
package org.corant.suites.elastic;

import static org.corant.shared.util.StringUtils.isNoneBlank;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.kernel.util.ConfigUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.ClassPathResource;
import org.corant.shared.util.StreamUtils;
import org.eclipse.microprofile.config.Config;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:54:10
 *
 */
public class ElasticConfig {

  public static final String PREFIX = "elastic.";
  public static final String ES_CLU_NOD = ".cluster-nodes";
  public static final String ES_DOC_PATHS = ".document-paths";
  public static final String ES_SETTING_PATH = ".setting-path";
  public static final String ES_ADD_PRO = ".property";
  public static final String ES_IDX_VER = ".index-version";
  public static final String ES_AUTO_UPDATE_SCHEMA = ".auto-update-schame";

  private String clusterName;
  private String clusterNodes;
  private String documentPaths;
  private String indexVersion;
  private boolean autoUpdateSchema = false;
  private Map<String, Object> setting = new LinkedHashMap<>();

  private final Map<String, String> properties = new HashMap<>();

  public static Map<String, ElasticConfig> from(Config config) {
    Map<String, ElasticConfig> map = new HashMap<>();
    Map<String, List<String>> cfgNmes = ConfigUtils.getGroupConfigNames(config, PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      final ElasticConfig cfg = of(config, k, v);
      if (isNoneBlank(cfg.clusterName, cfg.clusterNodes)) {
        map.put(k, cfg);
      }
    });
    return map;
  }

  public static ElasticConfig of(Config config, String name, Collection<String> propertieNames) {
    final ElasticConfig cfg = new ElasticConfig();
    final String proPrefix = PREFIX + name + ES_ADD_PRO;
    final int proPrefixLen = proPrefix.length();
    Set<String> proCfgNmes = new HashSet<>();
    cfg.setClusterName(name);
    propertieNames.forEach(pn -> {
      if (pn.endsWith(ES_CLU_NOD)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setClusterNodes);
      } else if (pn.endsWith(ES_DOC_PATHS)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setDocumentPaths);
      } else if (pn.endsWith(ES_SETTING_PATH)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::initSetting);
      } else if (pn.endsWith(ES_IDX_VER)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setIndexVersion);
      } else if (pn.endsWith(ES_AUTO_UPDATE_SCHEMA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setAutoUpdateSchema);
      } else if (pn.startsWith(proPrefix) && pn.length() > proPrefixLen) {
        proCfgNmes.add(pn);// handle properties
      }
    });
    doParseProperties(config, proPrefix, proCfgNmes, cfg);
    return cfg;
  }

  static void doParseProperties(Config config, String proPrefix, Set<String> proCfgNmes,
      ElasticConfig esConfig) {
    if (!proCfgNmes.isEmpty()) {
      int len = proPrefix.length() + 1;
      for (String cfgNme : proCfgNmes) {
        config.getOptionalValue(cfgNme, String.class).ifPresent(s -> {
          String proName = cfgNme.substring(len);
          esConfig.properties.put(proName, s);
        });
      }
    }
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
    ElasticConfig other = (ElasticConfig) obj;
    if (clusterName == null) {
      if (other.clusterName != null) {
        return false;
      }
    } else if (!clusterName.equals(other.clusterName)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   *
   * @return the clusterNodes
   */
  public String getClusterNodes() {
    return clusterNodes;
  }

  /**
   *
   * @return the documentPaths
   */
  public String getDocumentPaths() {
    return documentPaths;
  }

  /**
   *
   * @return the indexVersion
   */
  public String getIndexVersion() {
    return indexVersion;
  }

  /**
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  /**
   *
   * @return the setting
   */
  public Map<String, Object> getSetting() {
    return Collections.unmodifiableMap(setting);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (clusterName == null ? 0 : clusterName.hashCode());
    return result;
  }

  /**
   *
   * @return the autoUpdateSchema
   */
  public boolean isAutoUpdateSchema() {
    return autoUpdateSchema;
  }

  protected void initSetting(String path) {
    ClassPathResource setting = Resources.tryFromClassPath(path).findFirst().orElse(null);
    if (setting != null) {
      try (ByteArrayOutputStream os = new ByteArrayOutputStream();
          InputStream is = setting.openStream();) {
        StreamUtils.copy(is, os);
        this.setting.putAll(XContentHelper
            .convertToMap(new BytesArray(os.toByteArray()), true, XContentType.JSON).v2());
      } catch (IOException e) {
        throw new CorantRuntimeException(e, "%s not found in class path!", path);
      }
    }
  }

  protected Map<String, String> obtainProperties() {
    return properties;
  }

  /**
   *
   * @param autoUpdateSchema the autoUpdateSchema to set
   */
  protected void setAutoUpdateSchema(boolean autoUpdateSchema) {
    this.autoUpdateSchema = autoUpdateSchema;
  }

  /**
   *
   * @param clusterName the clusterName to set
   */
  protected void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   *
   * @param clusterNodes the clusterNodes to set
   */
  protected void setClusterNodes(String clusterNodes) {
    this.clusterNodes = clusterNodes;
  }

  /**
   *
   * @param documentPaths the documentPaths to set
   */
  protected void setDocumentPaths(String documentPaths) {
    this.documentPaths = documentPaths;
  }

  /**
   *
   * @param indexVersion the indexVersion to set
   */
  protected void setIndexVersion(String indexVersion) {
    this.indexVersion = indexVersion;
  }

}
