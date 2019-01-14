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
package org.corant.suites.elastic.metadata.resolver;

import static org.corant.shared.util.ClassUtils.primitiveToWrapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.corant.suites.elastic.metadata.ElasticMapping;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午5:03:30
 *
 */
public class JpaElasticMappingResolver extends DefaultElasticMappingResolver {

  @Override
  public ElasticMapping resolve(Class<?> documentClass) {
    return null;
  }

  protected Map<String, Object> genPrimitiveTypeDfltMapping(Class<?> cls) {
    Map<String, Object> map = new LinkedHashMap<>();
    Class<?> tcls = cls.isArray() ? cls.getComponentType() : cls;
    Class<?> acls = primitiveToWrapper(tcls);
    if (Boolean.class.isAssignableFrom(acls)) {
      map.put("type", "boolean");
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("null_value", false);
      map.put("store", false);
    } else if (Number.class.isAssignableFrom(acls)) {
      String esTypeName = acls.getSimpleName().toLowerCase(Locale.ENGLISH);
      if (acls.equals(BigDecimal.class) || acls.equals(Double.class) || acls.equals(Float.class)) {
        esTypeName = "double";
      } else if (acls.equals(BigInteger.class)) {
        esTypeName = "long";
      }
      map.put("type", esTypeName);
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("store", false);
      map.put("coerce", true);
      map.put("include_in_all", false);
      map.put("ignore_malformed", false);
    } else if (Character.class.isAssignableFrom(acls)) {
      map.put("type", "keyword");
      map.put("boost", 1.0f);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("ignore_above", 256);
      map.put("index_options", "docs");
      map.put("norms", false);
      // map.put("search_analyzer", "standard"); FIXME
      map.put("similarity", "classic");
    } else if (CharSequence.class.isAssignableFrom(acls)) {
      map.put("type", "text");
      map.put("boost", 1.0f);
      map.put("fielddata", false);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("index_options", "docs");
      map.put("norms", false);
      // map.put("position_increment_gap", 0); FIXME
      map.put("analyzer", "standard");
      map.put("search_analyzer", "standard");
      map.put("similarity", "classic");
      map.put("search_quote_analyzer", "standard");
      map.put("term_vector", "no");
    } else if (Temporal.class.isAssignableFrom(acls) || Date.class.isAssignableFrom(acls)) {
      map.put("type", "date");
      map.put("boost", 1.0f);
      map.put("doc_values", true);
      map.put("index", true);
      map.put("format", DATE_FMT);
      map.put("include_in_all", false);
      map.put("ignore_malformed", false);
      map.put("store", false);
    } else if (Enum.class.isAssignableFrom(acls) || acls.equals(Locale.class)
        || acls.equals(Class.class) || acls.equals(Currency.class) || acls.equals(TimeZone.class)
        || acls.equals(URI.class) || acls.equals(URL.class)) {
      map.put("type", "keyword");
      map.put("boost", 1.0f);
      map.put("index", true);
      map.put("include_in_all", false);
      map.put("store", false);
      map.put("eager_global_ordinals", false);
      map.put("ignore_above", 256);
      map.put("index_options", "docs");
      map.put("norms", false);
      map.put("search_analyzer", "standard");
      map.put("similarity", "classic");
    }
    return map;
  }

}
