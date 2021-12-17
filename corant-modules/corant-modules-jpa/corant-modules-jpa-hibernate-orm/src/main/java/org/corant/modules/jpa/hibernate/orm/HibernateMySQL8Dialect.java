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
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.type.StandardBasicTypes;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午2:05:36
 *
 */
public class HibernateMySQL8Dialect extends MySQL8Dialect {

  public static final int MAX_LENGTH = 4000;

  public HibernateMySQL8Dialect() {
    this.registerColumnType(Types.VARCHAR, "nvarchar($l)");
    this.registerColumnType(Types.VARCHAR, MAX_LENGTH, "nvarchar($l)");
    this.registerColumnType(Types.CLOB, "longtext");
    this.registerColumnType(Types.NCLOB, "longtext");
    this.registerColumnType(Types.LONGNVARCHAR, "longtext");
    this.registerColumnType(Types.NUMERIC, "decimal(19,6)");
    this.registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());
    this.registerHibernateType(Types.NCLOB, StandardBasicTypes.STRING.getName());
    this.registerHibernateType(Types.LONGNVARCHAR, StandardBasicTypes.STRING.getName());
  }

  @Override
  public NameQualifierSupport getNameQualifierSupport() {
    return NameQualifierSupport.NONE;
  }
}
