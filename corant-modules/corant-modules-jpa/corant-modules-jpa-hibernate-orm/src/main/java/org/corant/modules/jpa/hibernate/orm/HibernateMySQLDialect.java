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
package org.corant.modules.jpa.hibernate.orm;

import java.sql.Types;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午2:05:36
 */
public class HibernateMySQLDialect extends MySQLDialect {

  public HibernateMySQLDialect() {
    super(DatabaseVersion.make(5, 7));
  }

  @Override
  public NameQualifierSupport getNameQualifierSupport() {
    return NameQualifierSupport.NONE;
  }

  @Override
  public JdbcType resolveSqlTypeDescriptor(String columnTypeName, int jdbcTypeCode, int precision,
      int scale, JdbcTypeRegistry jdbcTypeRegistry) {

    switch (jdbcTypeCode) {
      case Types.VARCHAR -> jdbcTypeCode = StandardBasicTypes.NSTRING.getSqlTypeCode();
      case Types.CHAR -> jdbcTypeCode = StandardBasicTypes.CHARACTER_NCHAR.getSqlTypeCode();
      case Types.LONGNVARCHAR -> jdbcTypeCode = StandardBasicTypes.NTEXT.getSqlTypeCode();
      default -> {
        break;
      }
    }
    return super.resolveSqlTypeDescriptor(columnTypeName, jdbcTypeCode, precision, scale,
        jdbcTypeRegistry);
  }
}
