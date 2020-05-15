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

import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-dsa
 *
 * @author bingo 下午1:33:20
 *
 */
public class SquaredEuclideanDistance implements Distance {

  @Override
  public double calculate(Map<Object, Double> f1, Map<Object, Double> f2,
      Map<String, Double> hints) {
    if (f1 == null || f2 == null) {
      throw new CorantRuntimeException("Feature vectors can't be null");
    }
    double sum = 0;
    for (Object key : f1.keySet()) {
      Double v1 = f1.get(key);
      Double v2 = f2.get(key);
      if (v1 != null && v2 != null) {
        sum += Math.pow(v1 - v2, 2);
      }
    }
    return sum;
  }

}
