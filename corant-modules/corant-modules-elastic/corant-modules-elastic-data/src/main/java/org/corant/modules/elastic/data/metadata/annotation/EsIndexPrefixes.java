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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午7:03:39
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EsIndexPrefixes {

  EsIndexPrefixes DefaultEsIndexPrefixes = new EsIndexPrefixes() {

    @Override
    public Class<? extends Annotation> annotationType() {
      return EsIndexPrefixes.class;
    }

    @Override
    public int max_chars() {
      return 5;
    }

    @Override
    public int min_chars() {
      return 2;
    }
  };

  /**
   * must be greater than or equal to min_chars and less than 20, defaults to 5
   *
   * @return max_chars
   */
  int max_chars() default 5;

  /**
   * must be greater than zero, defaults to 2
   *
   * @return max_chars
   */
  int min_chars() default 2;
}
