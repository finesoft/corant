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
package org.corant.modules.bundle;

import java.util.Locale;
import java.util.function.BiFunction;

/**
 * corant-modules-bundle
 *
 * <p>
 * Interface for interpreting a raw message with a context parameters array and a locale into a
 * final message. It is recommended that all implementations be thread-safe.
 *
 * @author bingo 上午11:52:30
 */
@FunctionalInterface
public interface MessageInterpreter extends BiFunction<Object[], Locale, String> {
}
