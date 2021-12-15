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
package org.corant.modules.query;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;

/**
 * corant-modules-query-api
 *
 * @author bingo 上午10:27:41
 *
 */
public interface QueryStatistics {

  List<QueryStatisticsRecord> getHightFrequencyQueries();

  List<QueryStatisticsRecord> getSlowestEntranceQueries();

  List<QueryStatisticsRecord> getSlowestQueries();

  class QueryStatisticsRecord implements Comparable<QueryStatisticsRecord> {
    final String id = UUID.randomUUID().toString();
    final String key;
    final QueryParameter parameter;
    final Serializable script;
    final Instant occurredTime;
    final long uptime;
    final int resultSize;

    /**
     * @param key the query key, may be a query name
     * @param parameter the query parameter
     * @param script the query script
     * @param occurredTime the query occurred time
     * @param uptime the query execution uptime
     * @param resultSize the query result size
     */
    public QueryStatisticsRecord(String key, QueryParameter parameter, Serializable script,
        Instant occurredTime, long uptime, int resultSize) {
      this.key = key;
      this.parameter = parameter;
      this.occurredTime = occurredTime;
      this.script = script;
      this.uptime = uptime;
      this.resultSize = resultSize;
    }

    @Override
    public int compareTo(QueryStatisticsRecord o) {
      return Long.compareUnsigned(uptime, o.uptime);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      QueryStatisticsRecord other = (QueryStatisticsRecord) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      return true;
    }

    public String getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public Instant getOccurredTime() {
      return occurredTime;
    }

    public QueryParameter getParameter() {
      return parameter;
    }

    public int getResultSize() {
      return resultSize;
    }

    public Serializable getScript() {
      return script;
    }

    public long getUptime() {
      return uptime;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + (id == null ? 0 : id.hashCode());
    }

  }

  class QueryWatcher {
    String key;
    QueryParameter parameter;
    Serializable script;
    Instant occurredTime;
    long uptime;
    int resultSize;

    public static QueryWatcher start(String key, QueryParameter parameter) {
      QueryWatcher qw = new QueryWatcher();
      qw.key = key;
      qw.occurredTime = Instant.now();
      qw.parameter = new DefaultQueryParameter(parameter);
      return qw;
    }

    public QueryStatisticsRecord getRecord() {
      return new QueryStatisticsRecord(key, parameter, script, occurredTime, uptime, resultSize);
    }

    public QueryWatcher stop(Serializable script, int resultSize) {
      this.script = script;
      uptime = ChronoUnit.MILLIS.between(occurredTime, Instant.now());
      this.resultSize = resultSize;
      return this;
    }
  }
}
