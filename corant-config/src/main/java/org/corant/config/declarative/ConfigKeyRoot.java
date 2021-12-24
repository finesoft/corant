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
package org.corant.config.declarative;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.config.CorantConfigResolver.splitKey;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.config.Configs;

/**
 * corant-config
 *
 * Use to indicate the beginning of the namespace of the configuration item.
 * <p>
 * For example:
 *
 * <pre>
 * The declarative configuration object:
 *
 * &#64;ConfigKeyRoot(value = "datasource")
 * public class DatasourceConfig implements DeclarativeConfig {
 *   String url;
 *   Class<?> driver;
 * }
 *
 * The application properties:
 *
 * datasource.url = jdbc:sqlserver://localhost:1433;databaseName=example
 * datasource.driver = com.microsoft.sqlserver.jdbc.SQLServerXADataSource
 *
 * datasource.blog.url = jdbc:mysql://localhost:3306/blog
 * datasource.blog.driver = com.mysql.jdbc.jdbc2.optional.MysqlXADataSource
 *
 * </pre>
 *
 * All configuration item information whose key starts with "datasource" will be mapped to the
 * properties of the declarative configuration object DatasourceConfig, where the name between the
 * property name and "datasource" can be used as the name of the declarative configuration object.
 *
 * Assuming that the DataSource configuration object has two properties, the names are called "url"
 * and "driver", and the configuration items are named "datasource.blog.url" and
 * "datasource.blog.driver". In this case, the configuration item values of "datasource.blog.url"and
 * "datasource.blog.driver" will be mapped to the DatasourceConfig object named blog
 *
 * @author bingo 下午7:39:01
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface ConfigKeyRoot {

  String SPEC_KEY_ROOT_FMT = "corant.declarative-config-class.%s.key-root";
  String SPEC_KEY_ROOT_INDEX_FMT = "corant.declarative-config-class.%s.key-root-index";
  String SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT = "corant.declarative-config-class.%s.key-root-ignore";

  boolean ignoreNoAnnotatedItem() default true;

  int keyIndex() default -1;

  String value();

  class ConfigKeyRootLiteral extends AnnotationLiteral<ConfigKeyRoot> implements ConfigKeyRoot {

    private static final long serialVersionUID = 4676760366128922922L;

    final boolean ignoreNoAnnotatedItem;
    final int keyIndex;
    final String value;

    public ConfigKeyRootLiteral(boolean ignoreNoAnnotatedItem, int keyIndex, String value) {
      this.ignoreNoAnnotatedItem = ignoreNoAnnotatedItem;
      this.keyIndex = keyIndex;
      this.value = value;
    }

    public static ConfigKeyRoot of(Class<?> clazz) {
      String className = getUserClass(clazz).getCanonicalName();
      String rootCfgKey = String.format(SPEC_KEY_ROOT_FMT, className);
      String indexCfgKey = String.format(SPEC_KEY_ROOT_INDEX_FMT, className);
      String ignoreKey = String.format(SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT, className);
      String keyRoot = Configs.getValue(rootCfgKey, String.class);
      Integer keyIndex = Configs.getValue(indexCfgKey, Integer.class);
      String ignoreNotAnnItem = Configs.getValue(ignoreKey, String.class);
      ConfigKeyRoot ann = findAnnotation(clazz, ConfigKeyRoot.class, true);
      Boolean ignore = isNotBlank(ignoreNotAnnItem) ? toBoolean(ignoreNotAnnItem) : null;
      if (ann != null) {
        if (keyRoot == null) {
          keyRoot = ann.value();
        }
        if (keyIndex != null) {
          keyIndex = ann.keyIndex();
        }
        if (ignore == null) {
          ignore = ann.ignoreNoAnnotatedItem();
        }
      }
      if (keyRoot != null) {
        return new ConfigKeyRootLiteral(defaultObject(ignore, Boolean.FALSE),
            keyIndex == null || keyIndex < 0 ? splitKey(keyRoot).length : keyIndex, keyRoot);
      }
      return null;
    }

    @Override
    public boolean ignoreNoAnnotatedItem() {
      return ignoreNoAnnotatedItem;
    }

    @Override
    public int keyIndex() {
      return keyIndex;
    }

    @Override
    public String value() {
      return value;
    }

  }
}
