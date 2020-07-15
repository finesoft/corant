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
package org.corant.shared.util;

import static org.corant.shared.util.Lists.listOf;
import java.util.Iterator;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午8:03:08
 *
 */
public class IterablesTest extends TestCase {

  @SuppressWarnings("unchecked")
  @Test
  public void test() {
    Iterable<String> it1 = listOf("1", "2");
    Iterable<String> it2 = listOf("3", "4");
    Iterable<String> itAll = listOf("1", "2", "3", "4");
    Iterable<String> it1AndIt2 = listOf(Iterables.concat(it1, it2));
    assertEquals(itAll, it1AndIt2);
    Iterable<String> it1AndIt2or = listOf(Iterables.concat(it1.iterator(), it2.iterator()));
    assertEquals(itAll, it1AndIt2or);
    assertEquals(Iterables.get(it1, 1), "2");
    assertEquals(Iterables.get(it1AndIt2or, 2), "3");
    Iterable<Integer> tranItAll = Iterables.transform(itAll, Conversions::toInteger);
    assertEquals(Iterables.get(tranItAll, 0).intValue(), 1);
    assertEquals(Iterables.get(tranItAll, 1).intValue(), 2);
    assertEquals(Iterables.get(tranItAll, 2).intValue(), 3);
    assertEquals(Iterables.get(tranItAll, 3).intValue(), 4);
    Iterator<Integer> tranItorAll = Iterables.transform(itAll.iterator(), Conversions::toInteger);
    int i = 0;
    while (tranItorAll.hasNext()) {
      i++;
      assertEquals(i, tranItorAll.next().intValue());
    }
  }
}
