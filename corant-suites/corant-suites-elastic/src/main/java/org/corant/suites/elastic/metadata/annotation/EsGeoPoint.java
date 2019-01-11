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
package org.corant.suites.elastic.metadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:45:56
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsGeoPoint {

  GeoPointType type() default GeoPointType.TYPE_DFLT;

  public static enum GeoPointType {
    TYPE_DFLT("geo_point"), TYPE_AS_OBJ("Geo-point as an object"), TYPE_AS_STR(
        "Geo-point as a string"), TYPE_AS_HASH(
            "Geo-point as a geohash"), TYPE_AS_ARR("Geo-point as an array");

    final String type;

    private GeoPointType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }

  }
}
