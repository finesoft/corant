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

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-dsa
 *
 * @author bingo 下午1:45:22
 *
 */
public class CustomizedDistance implements Distance {

  public static void main(String... f2) {
    double d = 4;
    double p = 1 / 2;
    double x = 0.5;
    System.out.println(Double.compare(x, p));
    System.out.println(Math.pow(d, 1.0 / 2));
  }

  @Override
  public double calculate(Map<Object, Double> f1, Map<Object, Double> f2,
      Map<String, Double> hints) {
    Double p = defaultObject(hints.get("p"), Double.valueOf(1));
    Double q = defaultObject(hints.get("q"), Double.valueOf(1));
    if (f1 == null || f2 == null) {
      throw new CorantRuntimeException("Feature vectors can't be null");
    }
    double sum = 0;
    for (Object key : f1.keySet()) {
      Double v1 = f1.get(key);
      Double v2 = f2.get(key);
      if (v1 != null && v2 != null) {
        sum += Math.pow(Math.abs(v1 - v2), p);
      }
    }
    return Math.pow(sum, 1.0 / q);
  }

}
