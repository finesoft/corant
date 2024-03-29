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
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.el.ELManager;
import javax.el.ExpressionFactory;
import javax.el.StandardELContext;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.resource.URLResource;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Randoms;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import org.corant.shared.util.Texts;

/**
 * corant-config
 *
 * @author bingo 下午4:02:23
 *
 */
public class ConfigELProcessor {

  static final ThreadLocal<ELManager> elManagers = new ThreadLocal<>();
  final ConfigSourceBean sourceBean;
  final ResourceBean resourceBean;

  public ConfigELProcessor(Function<String, String> provider) {
    sourceBean = new ConfigSourceBean(provider);
    resourceBean = new ResourceBean();
  }

  public String evalValue(final String value) {
    final ExpressionFactory expressionFactory = ELManager.getExpressionFactory();
    final StandardELContext context = getElManager().getELContext();
    if (value.startsWith("fn:")) {
      return asString(expressionFactory.createMethodExpression(context,
          "${".concat(value.substring(3)).concat("}"), String.class, new Class[] {String.class})
          .invoke(context, Objects.EMPTY_ARRAY), null);
    } else {
      return asString(expressionFactory
          .createValueExpression(context, "${".concat(value).concat("}"), String.class)
          .getValue(context), null);
    }
  }

  protected ELManager getElManager() {
    if (elManagers.get() == null) {
      ELManager elm = new ELManager();
      elm.importClass(Randoms.class.getName());
      elm.importClass(Strings.class.getName());
      elm.importClass(Base64.class.getName());
      elm.importClass(UUID.class.getName());
      elm.importClass(Defaults.class.getName());
      elm.importClass(Names.class.getName());
      elm.importClass(ConfigNames.class.getName());
      elm.importClass(Conversions.class.getName());
      elm.importClass(TimeZone.class.getName());
      elm.importClass(Currency.class.getName());
      elm.importClass(Locale.class.getName());
      elm.importClass(Systems.class.getName());
      elm.setVariable("source", ELManager.getExpressionFactory().createValueExpression(sourceBean,
          ConfigSourceBean.class));
      elm.setVariable("resource",
          ELManager.getExpressionFactory().createValueExpression(resourceBean, ResourceBean.class));
      elManagers.set(elm);
    }
    return elManagers.get();
  }

  /**
   * corant-config
   *
   * @author bingo 下午11:58:51
   *
   */
  public static class ConfigSourceBean {

    final Function<String, String> provider;

    ConfigSourceBean(Function<String, String> provider) {
      this.provider = provider;
    }

    public String get(String propertyName) {
      return provider.apply(propertyName);
    }
  }

  /**
   * corant-config
   * <p>
   * A resource resolver use to get text/plain content
   *
   * @author bingo 上午11:21:08
   *
   */
  public static class ResourceBean {

    public String get(String propertyName) {
      if (isNotBlank(propertyName)) {
        try (Stream<URLResource> res = Resources.from(propertyName.strip())) {
          Optional<URLResource> re = res.findFirst();
          if (re.isPresent()) {
            try (InputStream is = re.get().openInputStream()) {
              return Texts.fromInputStream(is);
            }
          }
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      }
      return null;
    }
  }
}
