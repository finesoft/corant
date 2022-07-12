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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Services;

/**
 * corant-modules-datasource-shared
 * <p>
 * This class hack {@link BasicRowProcessor} to supports some additional field conversion, if there
 * is infringement, please inform me(finesoft@gmail.com).
 *
 * @author bingo 上午12:24:17
 *
 */
public class DbUtilBasicRowProcessor extends BasicRowProcessor {

  public static final DbUtilBasicRowProcessor INST = new DbUtilBasicRowProcessor();

  public static final List<DbUtilBasicFieldProcessor> FIELD_PROCESSORS =
      Services.selectRequired(DbUtilBasicFieldProcessor.class).sorted(Sortable::compare)
          .collect(Collectors.toList());

  @Override
  public Map<String, Object> toMap(ResultSet rs) throws SQLException {
    Map<String, Object> result = new CaseInsensitiveHashMap();
    ResultSetMetaData rsmd = rs.getMetaData();
    int cols = rsmd.getColumnCount();

    for (int i = 1; i <= cols; i++) {
      String columnName = rsmd.getColumnLabel(i);
      int type = rsmd.getColumnType(cols);
      if (null == columnName || 0 == columnName.length()) {
        columnName = rsmd.getColumnName(i);
      }
      boolean converted = false;
      for (DbUtilBasicFieldProcessor fp : FIELD_PROCESSORS) {
        if (fp.supports(columnName, type)) {
          result.put(columnName, fp.convert(rs, i));
          converted = true;
          break;
        }
      }
      if (!converted) {
        result.put(columnName, rs.getObject(i));
      }
    }

    return result;
  }

  /**
   * A Map that converts all keys to lowercase Strings for case insensitive lookups. This is needed
   * for the toMap() implementation because databases don't consistently handle the casing of column
   * names.
   *
   * <p>
   * The keys are stored as they are given [BUG #DBUTILS-34], so we maintain an internal mapping
   * from lowercase keys to the real keys in order to achieve the case insensitive lookup.
   *
   * <p>
   * Note: This implementation does not allow {@code null} for key, whereas {@link LinkedHashMap}
   * does, because of the code:
   *
   * <pre>
   * key.toString().toLowerCase()
   * </pre>
   */
  private static class CaseInsensitiveHashMap extends LinkedHashMap<String, Object> {
    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = -2848100435296897392L;

    /**
     * The internal mapping from lowercase keys to the real keys.
     *
     * <p>
     * Any query operation using the key ({@link #get(Object)}, {@link #containsKey(Object)}) is
     * done in three steps:
     * <ul>
     * <li>convert the parameter key to lower case</li>
     * <li>get the actual key that corresponds to the lower case key</li>
     * <li>query the map with the actual key</li>
     * </ul>
     * </p>
     */
    private final Map<String, String> lowerCaseMap = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(Object key) {
      Object realKey = lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
      return super.containsKey(realKey);
      // Possible optimisation here:
      // Since the lowerCaseMap contains a mapping for all the keys,
      // we could just do this:
      // return lowerCaseMap.containsKey(key.toString().toLowerCase());
    }

    /** {@inheritDoc} */
    @Override
    public Object get(Object key) {
      Object realKey = lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
      return super.get(realKey);
    }

    /** {@inheritDoc} */
    @Override
    public Object put(String key, Object value) {
      /*
       * In order to keep the map and lowerCaseMap synchronized, we have to remove the old mapping
       * before putting the new one. Indeed, oldKey and key are not necessaliry equals. (That's why
       * we call super.remove(oldKey) and not just super.put(key, value))
       */
      Object oldKey = lowerCaseMap.put(key.toLowerCase(Locale.ENGLISH), key);
      Object oldValue = super.remove(oldKey);
      super.put(key, value);
      return oldValue;
    }

    /** {@inheritDoc} */
    @Override
    public void putAll(Map<? extends String, ?> m) {
      for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        put(key, value);
      }
    }

    /** {@inheritDoc} */
    @Override
    public Object remove(Object key) {
      Object realKey = lowerCaseMap.remove(key.toString().toLowerCase(Locale.ENGLISH));
      return super.remove(realKey);
    }
  }
}
