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

import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.kernel.util.Qualifiers.NamedObject;
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
@ConfigKeyRoot("elastic")
public class ElasticConfig implements NamedObject, DeclarativeConfig {

  @ConfigKeyItem
  protected String clusterName;

  @ConfigKeyItem
  protected String clusterNodes;

  @ConfigKeyItem
  protected String documentPaths;

  @ConfigKeyItem
  protected String indexVersion;

  @ConfigKeyItem(defaultValue = "false")
  protected Boolean autoUpdateSchema = false;

  @ConfigKeyItem
  protected Optional<String> settingPath;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> properties = new HashMap<>();

  protected Map<String, Object> setting = new LinkedHashMap<>();

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

  @Override
  public String getName() {
    return clusterName;
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

  /**
   *
   * @return the settingPath
   */
  public Optional<String> getSettingPath() {
    return settingPath;
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

  @Override
  public boolean isValid() {
    return isNoneBlank(getClusterName(), getClusterNodes());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    if (isBlank(clusterName)) {
      clusterName = key;
    }
    getSettingPath().ifPresent(path -> {
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
    });
  }

}
