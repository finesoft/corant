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
import java.util.Collection;
import javax.el.ExpressionFactory;
import javax.el.StandardELContext;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Randoms;
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
  ConfigSourceBean source;
  StandardELContext context;

  ConfigVariableProcessor(Collection<ConfigSource> configSources) {
    source = new ConfigSourceBean(configSources);
    expressionFactory = new ExpressionFactoryImpl();
    context = new StandardELContext(expressionFactory);
    context.getVariableMapper().setVariable("source",
        expressionFactory.createValueExpression(source, ConfigSourceBean.class));

  }

  String evalValue(final String key) {
    if (key.startsWith("fn:")) {
      return asString(expressionFactory.createMethodExpression(context,
          "${".concat(key.substring(3)).concat("}"), String.class, new Class[] {String.class})
          .invoke(context, Objects.EMPTY_ARRAY), null);
    } else {
      return asString(expressionFactory
          .createValueExpression(context, "${".concat(key).concat("}"), String.class)
          .getValue(context), null);
    }
  }

  String getValue(final String key) {
    return source.get(key);
  }

  public static class ConfigSourceBean {

    protected final Collection<ConfigSource> configSources;

    public ConfigSourceBean(Collection<ConfigSource> configSources) {
      super();
      this.configSources = configSources;
    }

    public String get(String propertyName) {
      for (ConfigSource cs : configSources) {
        String value = cs.getValue(propertyName);
        if (isNotBlank(value)) {
          return value;
        }
      }
      return null;
    }

    public String randomDouble(double max) {
      return Double.toString(Randoms.randomDouble(max));
    }

    public String randomDouble(double min, double max) {
      return Double.toString(Randoms.randomDouble(min, max));
    }

    public String randomFloat(float max) {
      return Float.toString(Randoms.randomFloat(max));
    }

    public String randomFloat(float min, float max) {
      return Float.toString(Randoms.randomFloat(min, max));
    }

    public String randomInt(int max) {
      return Integer.toString(Randoms.randomInt(max));
    }

    public String randomInt(int min, int max) {
      return Integer.toString(Randoms.randomInt(min, max));
    }

    public String randomLetters(int length) {
      return Randoms.randomLetters(length);
    }

    public String randomLetters(int length, boolean upperCase) {
      if (upperCase) {
        return Randoms.randomUpperCaseLetters(length);
      } else {
        return Randoms.randomLowerCaseLetters(length);
      }
    }

    public String randomLong(long max) {
      return Long.toString(Randoms.randomLong(max));
    }

    public String randomLong(long min, long max) {
      return Long.toString(Randoms.randomLong(min, max));
    }

    public String randomNumAndLetter(int length) {
      return Randoms.randomNumbersAndLetters(length);
    }

    public String randomNumAndLetter(int length, boolean upperCase) {
      if (upperCase) {
        return Randoms.randomNumbersAndUcLetters(length);
      } else {
        return Randoms.randomNumbersAndLcLetters(length);
      }
    }

    public String randomNumbers(int length) {
      return Randoms.randomNumbers(length);
    }

  }

}
