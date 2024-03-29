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
package org.corant.modules.datasource.shared.util;

import static org.corant.shared.util.Conversions.toObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午12:03:04
 *
 */
public class DbUtilBlobFieldProcessor implements DbUtilBasicFieldProcessor {

  @Override
  public Object convert(ResultSet rs, int colIndex, Object... hints) throws SQLException {
    return toObject(rs.getBlob(colIndex), byte[].class);
  }

  @Override
  public boolean supports(String fieldName, int sqlFieldType, Object... hints) {
    return sqlFieldType == Types.BLOB;
  }

}
