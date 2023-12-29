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
package org.corant.modules.keycloak.event.selector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.keycloak.Config.Scope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;

/**
 * corant-modules-keycloak-event
 *
 * @author bingo 下午8:20:49
 */
public class AbstactSelector {

  static final Set<Integer> REG_CHARS =
      ".^$+{[]|()".chars().mapToObj(Integer::valueOf).collect(Collectors.toSet());
  static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();
  final List<Map<String, Object>> conditions = new ArrayList<>();
  final ObjectMapper objectMapper = new ObjectMapper();

  AbstactSelector(Scope scope) {
    if (scope != null) {
      String conditionstr = scope.get("event-conditions");
      if (conditionstr != null && conditionstr.trim().length() > 0) {
        try {
          @SuppressWarnings("rawtypes")
          List<Map> list = objectMapper.readValue(conditionstr,
              objectMapper.getTypeFactory().constructParametricType(List.class, Map.class));
          if (list != null && !list.isEmpty()) {
            list.forEach(conditions::add);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  Boolean getMapBoolean(Map<?, ?> cmd, String key) {
    if (cmd == null) {
      return Boolean.FALSE;
    }
    Object obj = cmd.get(key);
    return obj == null ? Boolean.FALSE : Boolean.valueOf(obj.toString());
  }

  Long getMapLong(Map<?, ?> cmd, String key) {
    if (cmd == null) {
      return null;
    }
    Object obj = cmd.get(key);
    return obj == null ? null : Long.valueOf(obj.toString());
  }

  String getMapString(Map<?, ?> cmd, String key) {
    if (cmd == null) {
      return null;
    }
    Object obj = cmd.get(key);
    return obj == null ? null : obj.toString();
  }

  boolean hasRegexChar(String cond) {
    return cond != null && cond.trim().length() > 0 && cond.chars().anyMatch(REG_CHARS::contains);
  }

  boolean matchLong(Supplier<Long> supplier, Map<?, ?> cmd, String key) {
    Long cand = supplier.get();
    String conds = getMapString(cmd, key);
    NumAndCmpr nac = NumCmpr.parse(conds);
    if (nac.num == null) {
      return true;
    }
    return nac.cmpr.match(cand, nac.num);
  }

  boolean matchString(String cand, Map<?, ?> cmd, String key) {
    String cond = getMapString(cmd, key);
    if (cond == null || Objects.equal(cand, cond)) {
      return true;
    } else if (cand == null) {
      return false;
    } else if (hasRegexChar(cond)) {
      Pattern p = PATTERNS.computeIfAbsent(cond,
          c -> Pattern.compile(cond, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
      if (p != null) {
        return p.matcher(cand).matches();
      } else {
        return false;
      }
    } else {
      return cand.equalsIgnoreCase(cond);
    }
  }

  public enum NumCmpr {

    GT, LT, EQ, GTE, LTE;

    public static NumAndCmpr parse(String conds) {
      if (conds == null || conds.trim().length() < 1) {
        return new NumAndCmpr(null, null);
      }
      for (NumCmpr nc : NumCmpr.values()) {
        String exp = nc.express();
        int expLen = exp.length();
        if (conds.toLowerCase(Locale.ROOT).startsWith(exp) && conds.length() > expLen) {
          return new NumAndCmpr(Long.valueOf(conds.substring(expLen)), nc);
        }
      }
      return new NumAndCmpr(Long.valueOf(conds), NumCmpr.EQ);
    }

    public String express() {
      return name() + " ";
    }

    public boolean match(Long cand, Long cond) {
      if (cond == null) {
        return true;
      } else if (cand == null) {
        return false;
      } else {
        switch (this) {
          case GT:
            return cand.longValue() > cond.longValue();
          case LT:
            return cand.longValue() < cond.longValue();
          case GTE:
            return cand.longValue() >= cond.longValue();
          case LTE:
            return cand.longValue() <= cond.longValue();
          default:
            return cand.equals(cond);
        }
      }
    }
  }

  static class NumAndCmpr {
    final Long num;
    final NumCmpr cmpr;

    protected NumAndCmpr(Long num, NumCmpr cmpr) {
      this.num = num;
      this.cmpr = cmpr;
    }
  }
}
