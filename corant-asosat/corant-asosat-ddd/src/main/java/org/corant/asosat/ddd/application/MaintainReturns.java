/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:14:38
 *
 */
package org.corant.asosat.ddd.application;


import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.json.JsonObject;
import org.corant.asosat.ddd.domain.model.AbstractVersionedAggregationReference;
import org.corant.asosat.ddd.domain.model.AbstractGenericAggregation;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;
import org.corant.suites.ddd.model.Readable;

public class MaintainReturns extends AbstractValueObject
    implements Readable<MaintainReturns, JsonObject> {

  private static final long serialVersionUID = 5727350117141714262L;

  final Long id;

  final long vn;

  private MaintainReturns(Long id, long vn) {
    super();
    this.id = id;
    this.vn = vn;
  }

  public static MaintainReturns of(AbstractVersionedAggregationReference<?> ref) {
    return new MaintainReturns(ref.getId(), ref.getVn());
  }

  public static MaintainReturns of(AbstractGenericAggregation<?, ?> aggregation) {
    return new MaintainReturns(aggregation.getId(), aggregation.getVn());
  }

  public static MaintainReturns of(Long id, long vn) {
    return new MaintainReturns(id, vn);
  }

  public Long getId() {
    return id;
  }

  public long getVn() {
    return vn;
  }

  @Override
  public String toHumanReader(Locale locale) {
    return Readable.super.toHumanReader(locale);
  }

  @Override
  public JsonObject toJsonReader(Function<MaintainReturns, JsonObject> provider) {
    return Readable.super.toJsonReader(provider);
  }

  public Map<String, Object> toMap() {
    return mapOf("id", getId(), "vn", getVn());
  }

}
