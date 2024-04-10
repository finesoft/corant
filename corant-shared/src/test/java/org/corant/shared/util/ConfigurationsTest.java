/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Configurations.getConfigValue;
import java.util.NoSuchElementException;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 14:02:43
 */
public class ConfigurationsTest extends TestCase {

  @Test
  public void testGetConfigValue() {
    Systems.setProperty("a.b.c", "123");
    Systems.setProperty("123.d", "1234");
    Systems.setProperty("var", "${a.b.c}");
    Systems.setProperty("varDefault", "${a.b:0}");
    Systems.setProperty("varCompose1", "${a.b.c}.d");
    Systems.setProperty("varCompose2", "${${a.b.c}.d}");
    Systems.setProperty("varCompose3", "${${a.b.c}.d}+${a.b.c}");
    Systems.setProperty("varCompose4", "${${a.b.c}.d}+${a.b:\\:\\:}");
    Systems.setProperty("varNotExist", "${a.b}");
    assertNull(getConfigValue("a.b", String.class));
    assertEquals(getConfigValue("a.b.c", String.class), "123");
    assertEquals(getConfigValue("var", String.class), "123");
    assertEquals(getConfigValue("varDefault", String.class), "0");
    assertEquals(getConfigValue("varCompose1", String.class), "123.d");
    assertEquals(getConfigValue("varCompose2", String.class), "1234");
    assertEquals(getConfigValue("varCompose3", String.class), "1234+123");
    assertEquals(getConfigValue("varCompose4", String.class), "1234+::");
    try {
      getConfigValue("varNotExist", String.class);
    } catch (Exception e) {
      assert e instanceof NoSuchElementException;
    }
  }
}
