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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.config.CorantConfigResolver.dashify;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.config.Configs;
import org.corant.shared.util.Strings;

/**
 * corant-config
 *
 * @author bingo 下午7:39:01
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface ConfigKeyItem {

  String SPEC_KEY_ITEM_FMT = "corant.declarative-config-class.%s.key-item.%s";
  String SPEC_KEY_ITEM_DV_FMT = "corant.declarative-config-class.%s.key-item.%s.default-value";
  String SPEC_KEY_ITEM_PTN_FMT = "corant.declarative-config-class.%s.key-item.%s.pattern";

  String NO_DFLT_VALUE = "$no_default_value$";

  String defaultValue() default NO_DFLT_VALUE;

  String name() default Strings.EMPTY;

  DeclarativePattern pattern() default DeclarativePattern.SUFFIX;

  class ConfigKeyItemLiteral extends AnnotationLiteral<ConfigKeyItem> implements ConfigKeyItem {

    private static final long serialVersionUID = -6856666130654737130L;

    final String defaultValue;
    final String name;
    final DeclarativePattern pattern;

    public ConfigKeyItemLiteral(String defaultValue, String name, DeclarativePattern pattern) {
      this.defaultValue = defaultValue;
      this.name = name;
      this.pattern = pattern;
    }

    public static ConfigKeyItem of(Field field) {
      String className = getUserClass(field.getDeclaringClass()).getCanonicalName();
      String fieldName = field.getName();
      String keyItemCfgKey = String.format(SPEC_KEY_ITEM_FMT, className, fieldName);
      String dfltValCfgKey = String.format(SPEC_KEY_ITEM_DV_FMT, className, fieldName);
      String ptnCfgKey = String.format(SPEC_KEY_ITEM_DV_FMT, className, fieldName);
      String keyItem = Configs.getValue(keyItemCfgKey, String.class);
      String defaultValue = Configs.getValue(dfltValCfgKey, String.class);
      DeclarativePattern pattern = Configs.getValue(ptnCfgKey, DeclarativePattern.class);
      ConfigKeyItem ann = field.getAnnotation(ConfigKeyItem.class);
      if (ann != null) {
        if (keyItem == null) {
          keyItem = ann.name();
        }
        if (defaultValue == null) {
          defaultValue = ann.defaultValue();
        }
        if (pattern == null) {
          pattern = ann.pattern();
        }
      }
      return new ConfigKeyItemLiteral(defaultObject(defaultValue, NO_DFLT_VALUE),
          isBlank(keyItem) ? dashify(fieldName) : keyItem,
          defaultObject(pattern, DeclarativePattern.SUFFIX));
    }

    @Override
    public String defaultValue() {
      return defaultValue;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public DeclarativePattern pattern() {
      return pattern;
    }

  }
}
