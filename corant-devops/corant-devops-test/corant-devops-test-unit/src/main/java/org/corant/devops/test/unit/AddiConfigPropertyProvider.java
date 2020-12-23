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
package org.corant.devops.test.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.corant.shared.normal.Priorities;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * corant-devops-test-unit
 *
 * @author bingo 上午11:32:33
 *
 */
public class AddiConfigPropertyProvider implements ConfigSourceProvider {

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
    return Collections.singleton(new TestConfigSource());
  }

  /**
   * corant-devops-test-unit
   *
   * @author bingo 下午8:59:30
   *
   */
  private static final class TestConfigSource implements ConfigSource {
    Map<String, String> map = new HashMap<>(CorantJunit4Runner.ADDI_CFG_PROS.get());

    @Override
    public String getName() {
      return "CorantJunit4Runner_Additional_Config";
    }

    @Override
    public int getOrdinal() {
      return Priorities.ConfigPriorities.APPLICATION_ADJUST_ORDINAL;
    }

    @Override
    public Map<String, String> getProperties() {
      return map;
    }

    @Override
    public String getValue(String propertyName) {
      return map.get(propertyName);
    }
  }

}
