/*
 * Copyright (c) 2013-2018. BIN.CHEN
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
package org.corant.modules.flyway;

import static org.corant.shared.util.Sets.setOf;
import java.util.Collection;
import java.util.Set;
import javax.sql.DataSource;

/**
 * corant-modules-flyway
 *
 *
 * Simple base flyway config provider include datasource and locations
 *
 * @author bingo 下午10:17:16
 */
public interface FlywayConfigProvider {

  DataSource getDataSource();

  Collection<String> getLocations();

  class DefaultFlywayConfigProvider implements FlywayConfigProvider {

    final DataSource dataSource;
    final Set<String> locations;

    DefaultFlywayConfigProvider(DataSource dataSource, Set<String> locations) {
      this.dataSource = dataSource;
      this.locations = locations;
    }

    public static DefaultFlywayConfigProvider of(String name, DataSource dataSource) {
      return new DefaultFlywayConfigProvider(dataSource, setOf(name));
    }

    @Override
    public DataSource getDataSource() {
      return dataSource;
    }

    @Override
    public Set<String> getLocations() {
      return locations;
    }

  }
}
