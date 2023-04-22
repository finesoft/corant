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
package org.corant.modules.javafx.cdi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Strings.isBlank;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ResourceBundle;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.corant.config.Configs;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午11:08:06
 *
 */
public class CorantFXMLLoaderFactory {

  @Produces
  @Dependent
  @CorantFXML
  protected FXMLLoader produce(final InjectionPoint ip) {
    final CorantFXML fxml = (CorantFXML) ip.getQualifiers().stream()
        .filter(q -> q instanceof CorantFXML).findFirst().orElse(null);
    final URL url = fxml == null || isBlank(fxml.url()) ? null
        : toObject(Configs.assemblyStringConfigProperty(fxml.url()), URL.class);
    final ResourceBundle bundle = fxml == null || isBlank(fxml.bundle()) ? null
        : ResourceBundle.getBundle(Configs.assemblyStringConfigProperty(fxml.bundle()));
    final Charset charset = fxml == null || isBlank(fxml.charset()) ? UTF_8
        : Charset.forName(Configs.assemblyStringConfigProperty(fxml.charset()));

    return FXMLLoaders.builder().location(url).builderFactory(new JavaFXBuilderFactory())
        .charset(charset).resources(bundle).build();
  }

}
