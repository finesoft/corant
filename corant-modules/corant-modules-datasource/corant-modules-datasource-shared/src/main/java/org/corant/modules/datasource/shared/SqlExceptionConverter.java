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
package org.corant.modules.datasource.shared;

import java.sql.SQLException;
import jakarta.enterprise.context.ApplicationScoped;
import org.corant.shared.exception.GeneralRuntimeException;
import org.corant.shared.exception.GeneralRuntimeExceptionConverter;
import org.corant.shared.ubiquity.Experimental;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午10:59:56
 */
@Experimental
@ApplicationScoped
public class SqlExceptionConverter implements GeneralRuntimeExceptionConverter {

  public static final String SQLSTATE_MSG_KEY_PREFIX = "SQL.state.";

  @Override
  public GeneralRuntimeException convert(Throwable throwable, Object... parameters) {
    SQLException sqlException = (SQLException) throwable;
    return new GeneralRuntimeException(sqlException,
        SQLSTATE_MSG_KEY_PREFIX + sqlException.getSQLState(), parameters);
  }

  @Override
  public boolean supports(Throwable throwable) {
    return throwable instanceof SQLException && ((SQLException) throwable).getSQLState() != null;
  }

}
