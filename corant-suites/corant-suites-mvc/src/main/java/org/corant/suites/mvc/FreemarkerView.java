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

import org.jboss.resteasy.plugins.providers.html.View;

/**
 * corant-suites-mvc
 *
 * @author bingo 下午2:36:53
 *
 */
public class FreemarkerView extends View {

  /**
   * @param path
   */
  FreemarkerView(String path) {
    super(path);
  }

  /**
   * @param path
   * @param model
   */
  FreemarkerView(String path, Object model) {
    super(path, model);
  }

  /**
   * @param path
   * @param model
   * @param modelName
   */
  FreemarkerView(String path, Object model, String modelName) {
    super(path, model, modelName);
  }

}
