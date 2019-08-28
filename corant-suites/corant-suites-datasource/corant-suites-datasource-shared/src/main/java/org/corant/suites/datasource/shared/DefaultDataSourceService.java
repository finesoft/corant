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
package org.corant.suites.datasource.shared;

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.isNotBlank;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.kernel.api.DataSourceService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午2:44:36
 *
 */
@ApplicationScoped
public class DefaultDataSourceService implements DataSourceService {

  @Override
  public DataSource get(String name) {
    if (isNotBlank(name) && name.startsWith(DataSourceConfig.JNDI_SUBCTX_NAME)) {
      try {
        return forceCast(new InitialContext().lookup(name));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      return resolveNamed(DataSource.class, name).orElseThrow(
          () -> new CorantRuntimeException("Can not find any data source named %s", name));
    }
  }

}
