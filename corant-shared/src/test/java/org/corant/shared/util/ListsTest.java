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
package org.corant.shared.util;

import static org.corant.shared.util.Lists.append;
import static org.corant.shared.util.Lists.removeIf;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午5:10:41
 *
 */
public class ListsTest extends TestCase {

  @Test
  public void testArrayAppend() {
    String[] array = new String[] {"a", "b", "c"};
    String[] appendArray = new String[] {"a", "b", "c", "d"};
    assertArrayEquals(append(array, "d"), appendArray);
  }

  @Test
  public void testArrayRemove() {
    String[] array = new String[] {"a"};
    String[] removedArray = new String[0];
    assertArrayEquals(removeIf(array, x -> x.equals("a")), removedArray);
    assertArrayEquals(removeIf(array, x -> x.equals("x")), array);
  }

}