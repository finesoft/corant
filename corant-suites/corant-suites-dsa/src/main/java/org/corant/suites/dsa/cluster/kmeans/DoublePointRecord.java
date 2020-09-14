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
package org.corant.suites.dsa.cluster.kmeans;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.linkedHashMapOf;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-dsa
 *
 * @author bingo 上午9:58:33
 *
 */
public class DoublePointRecord implements Clusterable {

  private final Object id;

  private final double[] point;

  private final TreeMap<Object, Double> features = new TreeMap<>();

  private Map<Object, Object> descriptions = new HashMap<>();

  /**
   * @param id
   * @param point
   */
  public DoublePointRecord(Object id, double[] point) {
    super();
    this.id = id;
    this.point = Arrays.copyOf(point, point.length);
  }

  /**
   * @param id
   * @param features
   * @param comparator
   */
  public <T> DoublePointRecord(Object id, Map<T, Double> features, Comparator<T> comparator) {
    this(id, asTreeMap(features, comparator));
  }

  /**
   * @param id
   * @param features
   */
  public DoublePointRecord(Object id, TreeMap<?, Double> features) {
    super();
    this.id = id;
    if (features != null) {
      shouldNotNull(features.comparator(),
          () -> new CorantRuntimeException("Only accept tree map with comparator!"));
      this.features.putAll(features);
    }
    Double[] vs = this.features.entrySet().stream().map(Entry::getValue).toArray(Double[]::new);
    point = new double[vs.length];
    Arrays.setAll(point, i -> vs[i]);
  }

  static <T> TreeMap<T, Double> asTreeMap(Map<T, Double> map, Comparator<T> comparator) {
    TreeMap<T, Double> t = new TreeMap<>(comparator);
    t.putAll(map);
    return t;
  }

  /**
   *
   * @return the descriptions
   */
  public Map<Object, Object> getDescriptions() {
    return descriptions;
  }

  /**
   *
   * @return the features
   */
  public TreeMap<Object, Double> getFeatures() {
    return features;
  }

  public Object getId() {
    return id;
  }

  @Override
  public double[] getPoint() {
    return Arrays.copyOf(point, point.length);
  }

  /**
   *
   * @param descriptions the descriptions to set
   */
  public void setDescriptions(Map<Object, Object> descriptions) {
    this.descriptions = descriptions;
  }

  public DoublePointRecord withDescriptions(Object... objects) {
    descriptions = linkedHashMapOf(objects);
    return this;
  }
}
