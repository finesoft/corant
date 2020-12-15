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
package org.corant.config.spi;

import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.util.Collection;
import javax.el.ExpressionFactory;
import javax.el.StandardELContext;
import org.corant.shared.util.Objects;
import org.eclipse.microprofile.config.spi.ConfigSource;
import com.sun.el.ExpressionFactoryImpl;

/**
 * corant-config
 *
 * NOTE: This class is no thread safe
 *
 * @author bingo 下午8:26:03
 *
 */
class ConfigVariableProcessor {

  ExpressionFactory expressionFactory;
  ConfigSourceBean bean;
  StandardELContext context;

  ConfigVariableProcessor(Collection<ConfigSource> configSources) {
    bean = new ConfigSourceBean(configSources);
    expressionFactory = new ExpressionFactoryImpl();
    context = new StandardELContext(expressionFactory);
    context.getVariableMapper().setVariable("source",
        expressionFactory.createValueExpression(bean, ConfigSourceBean.class));

  }

  String resolveValue(final String key) {
    if (key.startsWith("#") && key.length() > 1) {
      return asString(expressionFactory.createMethodExpression(context,
          "${".concat(key.substring(1)).concat("}"), String.class, new Class[] {String.class})
          .invoke(context, Objects.EMPTY_ARRAY), null);
    } else {
      return bean.getValue(key);
    }
  }

  void setExpressionFactory(ExpressionFactory expressionFactory) {
    this.expressionFactory = expressionFactory;
  }

  public static class ConfigSourceBean {

    protected final Collection<ConfigSource> configSources;

    public ConfigSourceBean(Collection<ConfigSource> configSources) {
      super();
      this.configSources = configSources;
    }

    public String getValue(String propertyName) {
      for (ConfigSource cs : configSources) {
        String value = cs.getValue(propertyName);
        if (isNotBlank(value)) {
          return value;
        }
      }
      return null;
    }

    public String splitValue(String propertyName, String spreator, int index) {
      return split(getValue(propertyName), spreator)[index];
    }
  }

}
