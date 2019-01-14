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
 * Fields of type geo_point accept latitude-longitude pairs, which can be used:
 *
 * <li>to find geo-points within a bounding box, within a certain distance of a central point, or
 * within a polygon.</li>
 * <li>to aggregate documents geographically or by distance from a central point.</li>
 * <li>to integrate distance into a document’s relevance score.</li>
 * <li>to sort documents by distance.</li>
 *
 *
 * <li>Geo-point expressed as an object, with lat and lon keys, such as {"lat":41.12,
 * "lon":-71.34}.</li>
 * <li>Geo-point expressed as a string with the format: "lat,lon", such as "41.12,-71.34"</li>
 * <li>Geo-point expressed as a geohash, such as "drm3btev3e86"</li>
 * <li>Geo-point expressed as an array with the format: [ lon, lat], such as [ -71.34, 41.12 ]</li>
 *
 * A geo-bounding box query which finds all geo-points that fall inside the box.
 *
 * @author bingo 上午11:45:56
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsGeoPoint {

  /**
   * If true, malformed geo-points are ignored. If false (default), malformed geo-points throw an
   * exception and reject the whole document.
   *
   * @return ignore_malformed
   */
  boolean ignore_malformed() default false;

  /**
   * If true (default) three dimension points will be accepted (stored in source) but only latitude
   * and longitude values will be indexed; the third dimension is ignored. If false, geo-points
   * containing any more than latitude and longitude (two dimensions) values throw an exception and
   * reject the whole document.
   *
   * @return ignore_z_value
   */
  boolean ignore_z_value() default true;

  /**
   * Accepts an geopoint value which is substituted for any explicit null values. Defaults to null,
   * which means the field is treated as missing.
   *
   * @return null_value
   */
  String null_value() default "";
}
