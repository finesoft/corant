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
package org.corant.suites.dsa.cluster.distance;

import java.util.HashMap;
import java.util.Map;

/**
 * corant-suites-dsa
 *
 * @author bingo 下午1:45:22
 *
 */
public class MinkowskiDistance extends CustomizedDistance {

  @Override
  public double calculate(Map<Object, Double> f1, Map<Object, Double> f2,
      Map<String, Double> hints) {
    Map<String, Double> useHints = new HashMap<>();
    if (hints != null) {
      useHints.putAll(hints);
      useHints.put("q", 1.0);
    }
    return super.calculate(f1, f2, useHints);
  }
}
