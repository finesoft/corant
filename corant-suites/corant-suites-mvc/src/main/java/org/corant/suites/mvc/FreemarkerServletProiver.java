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
package org.corant.suites.mvc;

import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.defaultString;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import org.corant.config.declarative.DeclarativeConfigResolver;
import org.corant.suites.servlet.WebMetaDataProvider;
import org.corant.suites.servlet.metadata.WebInitParamMetaData;
import org.corant.suites.servlet.metadata.WebServletMetaData;
import freemarker.ext.servlet.FreemarkerServlet;

/**
 * corant-suites-mvc
 *
 * @author bingo 下午9:10:22
 *
 */
@ApplicationScoped
public class FreemarkerServletProiver implements WebMetaDataProvider {

  @Override
  public Stream<WebServletMetaData> servletMetaDataStream() {
    String name = FreemarkerServlet.class.getSimpleName();
    FreemarkerServletConfig config =
        DeclarativeConfigResolver.resolveSingle(FreemarkerServletConfig.class);
    if (config != null) {
      WebInitParamMetaData[] params = streamOf(config.getInitParams())
          .map(e -> new WebInitParamMetaData(e.getKey(), e.getValue(), e.getValue()))
          .toArray(WebInitParamMetaData[]::new);
      String pattern = defaultString(config.getUrlPattrn());
      return streamOf(new WebServletMetaData(name, new String[] {pattern}, new String[] {pattern},
          1, params, true, null, null, null, name, FreemarkerServlet.class, null, null));
    }
    return Stream.empty();
  }

  protected WebServletMetaData resolveWebInitParamMetaDatas() {
    String name = FreemarkerServlet.class.getSimpleName();
    FreemarkerServletConfig config =
        DeclarativeConfigResolver.resolveSingle(FreemarkerServletConfig.class);
    if (config != null) {
      WebInitParamMetaData[] params = streamOf(config.getInitParams())
          .map(e -> new WebInitParamMetaData(e.getKey(), e.getValue(), e.getValue()))
          .toArray(WebInitParamMetaData[]::new);
      String pattern = defaultString(config.getUrlPattrn());
      return new WebServletMetaData(name, new String[] {pattern}, new String[] {pattern}, 1, params,
          true, null, null, null, name, FreemarkerServlet.class, null, null);
    }
    return null;
  }
}
