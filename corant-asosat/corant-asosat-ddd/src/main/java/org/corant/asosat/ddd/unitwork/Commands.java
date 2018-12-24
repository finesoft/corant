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
package org.corant.asosat.ddd.unitwork;

import java.lang.annotation.Annotation;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:26:37
 *
 */
public interface Commands {

  public static <C> void accept(C command, Annotation... annotations) {
    return;
  }

  public static <C, R> R apply(C command, Annotation... annotations) {
    return null;
  }

}
