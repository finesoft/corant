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
package org.corant.modules.datasource.agroal;

import static java.util.Collections.emptyList;
import java.util.List;
import org.corant.modules.datasource.shared.DataSourceConfig;
import org.corant.shared.ubiquity.Sortable;
import io.agroal.api.AgroalDataSourceListener;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;

/**
 * corant-modules-datasource-agroal
 *
 * @author bingo 下午8:10:27
 */
public interface AgroalCPDataSourceConfigurator extends Sortable {

  void configure(DataSourceConfig config, AgroalDataSourceConfigurationSupplier agroalConfig);

  default List<AgroalDataSourceListener> getListeners() {
    return emptyList();
  }

}
