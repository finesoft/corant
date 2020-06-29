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
package org.corant.suites.jta.narayana.extend;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Strings.split;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.sql.DataSource;
import com.arjuna.ats.arjuna.exceptions.FatalError;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午3:30:52
 *
 */
public class DomainDataSourceJDBCAccess implements JDBCAccess {

  private DataSource dataSource;
  private volatile String domainName;

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);
    return connection;
  }

  public String getDomainName() {
    return domainName;
  }

  @Override
  public void initialise(StringTokenizer tokenizer) {
    String[] domains = split(tokenizer.nextToken(), "=", true, true);
    shouldBeTrue(sizeOf(domains) == 2 && domains[0].equalsIgnoreCase("domainName"));
    domainName = domains[1];
    while (tokenizer.hasMoreElements()) {
      Map<String, String> configuration = new HashMap<>();
      while (tokenizer.hasMoreTokens()) {
        String[] split = tokenizer.nextToken().split("=");
        configuration.put(split[0], split[1].replace("\\equ", "="));
      }
      try {
        dataSource = (DataSource) Class.forName(configuration.remove("ClassName")).newInstance();
        Iterator<String> iterator = configuration.keySet().iterator();
        while (iterator.hasNext()) {
          String key = iterator.next();
          String value = configuration.get(key);
          Method method = null;
          try {
            method = dataSource.getClass().getMethod("set" + key, java.lang.String.class);
            // String replace = value.replace("\\semi", ";");
            method.invoke(dataSource, value.replace("\\semi", ";"));
          } catch (NoSuchMethodException nsme) {
            method = dataSource.getClass().getMethod("set" + key, int.class);
            method.invoke(dataSource, Integer.valueOf(value));
          }
        }
      } catch (Exception ex) {
        dataSource = null;
        throw new FatalError(toString() + " : " + ex, ex);
      }
    }
  }
}
