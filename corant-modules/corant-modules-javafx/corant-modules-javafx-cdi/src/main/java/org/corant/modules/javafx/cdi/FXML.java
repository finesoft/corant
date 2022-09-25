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

import static org.corant.shared.util.Strings.EMPTY;
import javax.enterprise.util.Nonbinding;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午11:26:29
 *
 */
public @interface FXML {

  /**
   * The name of the resources used to resolve resource key attribute values.
   */
  @Nonbinding
  String bundle();

  /**
   * The URL used to resolve relative path attribute values.
   */
  @Nonbinding
  String url() default EMPTY;
}
