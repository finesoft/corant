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

import java.util.Optional;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Configurations;
import junit.framework.TestCase;

/**
 * corant-config
 * <p>
 * Use Microprofile-config TCK testing
 *
 * @author bingo 下午5:43:22
 *
 */
public class CorantConfigResolverTest extends TestCase {

  public static void main(String[] args) {
    System.out
        .println(Configurations.getAssembledConfigValue("bongo-${corant.datasource.arw.username}"));
    System.out.println(Configurations.getAssembledConfigValue("bongo-"));
    System.out
        .println(Configurations.getConfigValue("corant.jpa.anncy.class-packages", String.class));
    System.out.println(Configurations
        .getConfigValue("corant.datasource.arw.max-size", new TypeLiteral<Optional<Integer>>() {})
        .get());
    System.out.println(
        Configurations.getConfigValue("corant.datasource.arw.connection-url", String.class));
    System.out.println(
        Configurations.getConfigValue("corant.jpa.anncy.class-packages2", String.class, "alt"));
    System.out.println("=".repeat(80));
    for (String key : Configurations.getConfig().getKeys()) {
      System.out.println(key + "\t" + Configurations.getConfigValue(key, String.class));
    }
  }
}
