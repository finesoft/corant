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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import org.corant.shared.normal.Defaults;
import org.corant.shared.util.Strings;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午11:26:29
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface CorantFXML {

  CorantFXML EMPTY = CorantFXMLLiteral.of();

  /**
   * The name of the resources used to resolve resource key attribute values.
   */
  @Nonbinding
  String bundle() default Strings.EMPTY;

  /**
   * the character set used by FXMLLoader
   */
  @Nonbinding
  String charset() default Strings.EMPTY;

  /**
   * The URL used to resolve relative path attribute values.
   */
  @Nonbinding
  String url() default Strings.EMPTY;

  class CorantFXMLLiteral extends AnnotationLiteral<CorantFXML> implements CorantFXML {

    private static final long serialVersionUID = 4686509228865568858L;

    private final String bundle;
    private final String url;
    private final String charset;

    CorantFXMLLiteral(String bundle, String url, String charset) {
      super();
      this.bundle = defaultString(bundle);
      this.url = defaultString(url);
      this.charset = defaultObject(charset, Defaults.DFLT_CHARSET_STR);
    }

    public static CorantFXMLLiteral of() {
      return of(null, null, null);
    }

    public static CorantFXMLLiteral of(String bundle) {
      return of(bundle, null);
    }

    public static CorantFXMLLiteral of(String bundle, String url) {
      return of(bundle, url, Defaults.DFLT_CHARSET_STR);
    }

    public static CorantFXMLLiteral of(String bundle, String url, String charset) {
      return new CorantFXMLLiteral(bundle, url, charset);
    }

    @Override
    public String bundle() {
      return bundle;
    }

    @Override
    public String charset() {
      return charset;
    }

    @Override
    public String url() {
      return url;
    }

  }
}
