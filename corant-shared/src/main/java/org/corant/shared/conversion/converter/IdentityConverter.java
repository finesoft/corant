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
package org.corant.shared.conversion.converter;

import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午4:56:55
 *
 */
public final class IdentityConverter<T> extends AbstractConverter<T, T> {

  public static final IdentityConverter<?> INSTANCE = new IdentityConverter<>();

  private IdentityConverter() {
  }

  @Override
  protected T convert(T value, Map<String, ?> hints) throws Exception {
    return value;
  }

}
