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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:22
 *
 */
public class SimplePrincipals implements Iterable<SimplePrincipal> {

  protected final Collection<SimplePrincipal> principals;

  public SimplePrincipals(Collection<SimplePrincipal> principals) {
    this.principals = principals == null ? Collections.emptyList()
        : Collections.unmodifiableCollection(principals);
  }

  @Override
  public Iterator<SimplePrincipal> iterator() {
    return principals.iterator();
  }

  public Stream<SimplePrincipal> stream() {
    return principals.stream();
  }

  public List<SimplePrincipal> toList() {
    return listOf(this);
  }
}
