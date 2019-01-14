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
package org.corant.suites.elastic.metadata.annotation.instance;

import java.lang.annotation.Annotation;
import org.corant.suites.elastic.metadata.annotation.EsBinary;

public class EsBinaryInstance implements EsBinary {

  public static final EsBinaryInstance DLFT_INSTANCE = new EsBinaryInstance();

  boolean doc_values;
  boolean store;
  String alias;

  public EsBinaryInstance() {}

  /**
   * @param doc_values
   * @param store
   */
  public EsBinaryInstance(boolean doc_values, boolean store) {
    super();
    this.doc_values = doc_values;
    this.store = store;
  }

  public EsBinaryInstance(EsBinary ann) {
    doc_values = ann.doc_values();
    store = ann.store();
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return EsBinary.class;
  }

  @Override
  public boolean doc_values() {
    return doc_values;
  }

  /**
   *
   * @return the alias
   */
  public String getAlias() {
    return alias;
  }

  @Override
  public boolean store() {
    return store;
  }

}
