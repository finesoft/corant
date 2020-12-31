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

import static org.corant.shared.util.Strings.escapedSplit;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 上午9:40:02
 *
 */
public class StringsTest extends TestCase {

  @Test
  public void testContainsAnyChars() {
    assertFalse(Strings.containsAnyChars(null, null));
    assertFalse(Strings.containsAnyChars("", ""));
    assertTrue(Strings.containsAnyChars(" ", "a b"));
    assertTrue(Strings.containsAnyChars("abc", "ab"));
    assertFalse(Strings.containsAnyChars("abc", "z"));
  }

  @Test
  public void testEscapeSplit() {
    String str = "a1\tb\\t2\tc3";
    assertArrayEquals(escapedSplit(str, "\\", "\t"), new String[] {"a1", "b\\t2", "c3"});
    str = "a1|b\\|2|c3";
    assertArrayEquals(escapedSplit(str, "\\", "|"), new String[] {"a1", "b|2", "c3"});
    str = "a1ssssb\\ssss2ssssc3";
    assertArrayEquals(escapedSplit(str, "\\", "ssss"), new String[] {"a1", "bssss2", "c3"});
  }

  @Test
  public void testEscapeSplitx() {
    String str = "a1\tb\\t2\tc3";
    assertArrayEquals(escapedSplit(str, "\\", "\t"), new String[] {"a1", "b\\t2", "c3"});
    str = "a1|b\\|2|c3";
    assertArrayEquals(escapedSplit(str, "\\", "|"), new String[] {"a1", "b|2", "c3"});
    str = "a1hhhhb\\hhhh2hhhhc3";
    assertArrayEquals(escapedSplit(str, "\\", "hhhh"), new String[] {"a1", "bhhhh2", "c3"});
  }
}
