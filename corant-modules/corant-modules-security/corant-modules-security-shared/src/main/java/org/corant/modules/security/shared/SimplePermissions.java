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
package org.corant.modules.security.shared;

import static org.corant.shared.util.Lists.listOf;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

public class SimplePermissions implements Iterable<SimplePermission> {

  final Collection<SimplePermission> perms;

  public SimplePermissions(Collection<SimplePermission> perms) {
    this.perms = Collections.unmodifiableCollection(perms);
  }

  public static SimplePermissions of(SimplePermission... perms) {
    return new SimplePermissions(listOf(perms));
  }

  public static SimplePermissions of(String... name) {
    return new SimplePermissions(
        Arrays.stream(name).map(SimplePermission::new).collect(Collectors.toList()));
  }

  @Override
  public Iterator<SimplePermission> iterator() {
    return perms.iterator();
  }

}
