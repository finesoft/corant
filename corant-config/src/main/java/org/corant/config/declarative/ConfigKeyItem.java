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
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.shared.util.Strings;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
/**
 * corant-config
 *
 * @author bingo 下午7:39:01
 *
 */
public @interface ConfigKeyItem {

  String NO_DFLT_VALUE = "$no_default_value$";

  ConfigKeyItem EMPTY = new ConfigKeyItemLiteral();

  String defaultValue() default NO_DFLT_VALUE;

  String name() default Strings.EMPTY;

  DeclarativePattern pattern() default DeclarativePattern.SUFFIX;

  class ConfigKeyItemLiteral extends AnnotationLiteral<ConfigKeyItem> implements ConfigKeyItem {

    private static final long serialVersionUID = -6856666130654737130L;

    @Override
    public String defaultValue() {
      return NO_DFLT_VALUE;
    }

    @Override
    public String name() {
      return Strings.EMPTY;
    }

    @Override
    public DeclarativePattern pattern() {
      return DeclarativePattern.SUFFIX;
    }

  }
}
