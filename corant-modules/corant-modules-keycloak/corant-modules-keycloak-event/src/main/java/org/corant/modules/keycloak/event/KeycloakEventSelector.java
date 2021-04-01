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
package org.corant.modules.keycloak.event;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * corant-modules-keycloak-event
 *
 * @author bingo 上午10:18:01
 *
 */
public interface KeycloakEventSelector<E> extends Predicate<E> {

  String FIRST_SP = "(?<!\\\\);";
  String SECOND_SP = "(?<!\\\\),";

  static String[] split(String str, String sp) {
    if (str == null) {
      return new String[0];
    } else {
      return Arrays.stream(str.split(sp)).filter(x -> x != null && !x.isEmpty())
          .toArray(String[]::new);
    }
  }

}
