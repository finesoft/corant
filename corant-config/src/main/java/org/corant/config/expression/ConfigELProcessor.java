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
package org.corant.config.expression;

import static org.corant.shared.util.Objects.asString;
import java.util.function.Function;
import javax.el.ExpressionFactory;
import javax.el.StandardELContext;
import org.corant.shared.util.Objects;
import com.sun.el.ExpressionFactoryImpl;

/**
 * corant-config
 *
 * @author bingo 下午4:02:23
 *
 */
public class ConfigELProcessor {

  final ExpressionFactory expressionFactory;
  final ConfigSourceBean sourceBean;
  final ThreadLocal<StandardELContext> contexts = new ThreadLocal<>();

  public ConfigELProcessor(Function<String, String> provider) {
    sourceBean = new ConfigSourceBean(provider);
    expressionFactory = new ExpressionFactoryImpl();
  }

  public String evalValue(final String value) {
    if (contexts.get() == null) {
      StandardELContext context = new StandardELContext(expressionFactory);
      context.getVariableMapper().setVariable("source",
          expressionFactory.createValueExpression(sourceBean, ConfigSourceBean.class));
      contexts.set(context);
    }
    if (value.startsWith("fn:")) {
      return asString(
          expressionFactory
              .createMethodExpression(contexts.get(), "${".concat(value.substring(3)).concat("}"),
                  String.class, new Class[] {String.class})
              .invoke(contexts.get(), Objects.EMPTY_ARRAY),
          null);
    } else {
      return asString(expressionFactory
          .createValueExpression(contexts.get(), "${".concat(value).concat("}"), String.class)
          .getValue(contexts.get()), null);
    }
  }

  public static class ConfigSourceBean {

    final Function<String, String> provider;

    /**
     * @param provider
     */
    ConfigSourceBean(Function<String, String> provider) {
      super();
      this.provider = provider;
    }

    public String get(String propertyName) {
      return provider.apply(propertyName);
    }
  }
}
