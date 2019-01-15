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
 * Arrays of objects Arrays of objects do not work as you would expect: you cannot query each object
 * independently of the other objects in the array. If you need to be able to do this then you
 * should use the nested datatype instead of the object datatype.
 *
 * An array may contain null values, which are either replaced by the configured null_value or
 * skipped entirely. An empty array [] is treated as a missing field — a field with no values
 *
 * @author bingo 下午5:41:20
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsAlias {

  String name();

}
