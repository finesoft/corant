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
package org.corant.config;

import static org.corant.shared.util.Maps.linkedHashMapOf;
import java.util.Map;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-config
 *
 * @author bingo 下午5:43:22
 *
 */
public class CorantConfigResolverTest extends TestCase {

  @Test
  public static void testResolveValue() {
    Map<String, String> map = linkedHashMapOf("a.b", "1234", "c.d", "${a.b}", "e.f",
        "5\\${}6${c.d}", "g.h", "56${c.d}.\\${", "i.j", "${c.e:8080}", "k.l", "b", "m.n",
        "mn${a.${k.l}}", "o.p", "${\\}", "r.s", "\\${}", "t.w",
        "#{a.b} ${g.h} \\${} ${c.d} m${a.${k.l}} ${a.${k.l}}n  x${a.#{k.l}:*}y ");
    map.put(Config.PROPERTY_EXPRESSIONS_ENABLED, "true");
    System.out.println("enabled:");
    map.forEach((k, v) -> {
      String resolved = String.format("%-8s %-80s %-128s", k, v,
          CorantConfigResolver.resolveValue(map.get(k), (x, y) -> {
            String val = map.get(y);
            if (x) {
              val = val.concat("[EL]");
            }
            return val;
          }));
      System.out.println(resolved);
    });
    System.out.println("disabled:");
    map.put(Config.PROPERTY_EXPRESSIONS_ENABLED, "false");
    map.forEach((k, v) -> {
      String resolved = String.format("%-8s %-80s %-128s", k, v,
          CorantConfigResolver.resolveValue(map.get(k), (x, y) -> {
            String val = map.get(y);
            if (x) {
              val = val.concat("[EL]");
            }
            return val;
          }));
      System.out.println(resolved);
    });
  }

  @Test
  public void test() {
    // System.setProperty("vehicle.name", "bingo");
    Config config = ConfigProvider.getConfig();
    // config.getConverter(OptionalInt.class).map(converter -> converter.convert(null))
    // .orElseThrow(NoSuchElementException::new);
    // for (String name : config.getPropertyNames()) {
    // System.out.println(name + "\t" + config.getValue(name, String.class));
    // }
    System.out.println(config.getValue("backslash.comma.string", String.class));
    System.out.println(config.getValue("server.url", String.class));
    System.out.println(config.getValue("vehicle.name", String.class));
    System.out.println(String.join(";", config.getValue("myPets", String[].class)));
  }
}
