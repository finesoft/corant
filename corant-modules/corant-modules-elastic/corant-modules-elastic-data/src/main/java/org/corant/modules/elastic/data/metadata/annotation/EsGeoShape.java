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
package org.corant.modules.elastic.data.metadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * corant-modules-elastic-data
 *
 * The geo_shape datatype facilitates the indexing of and searching with arbitrary geo shapes such
 * as rectangles and polygons. It should be used when either the data being indexed or the queries
 * being executed contain shapes other than just points.
 *
 * @author bingo 上午11:46:30
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsGeoShape {

  /**
   * The geo_shape mapping maps geo_json geometry objects to the geo_shape type. To enable it, users
   * must explicitly map fields to the geo_shape type.
   *
   * @return options
   */
  EsProperty[] options() default {};
}
