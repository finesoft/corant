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
package org.corant.suites.dsa.structure;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * corant-suites-dsa
 *
 * Unfinish yet
 *
 * @author bingo 上午11:34:26
 *
 */
public class TrieMap<V> extends AbstractMap<CharSequence, V>
    implements Serializable, Map<CharSequence, V> {

  private static final long serialVersionUID = -7138732430310925683L;

  @Override
  public Set<Entry<CharSequence, V>> entrySet() {
    return null;
  }

}
