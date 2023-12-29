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
package org.corant.modules.dsa.cluster.kmeans;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer.EmptyClusterStrategy;
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.evaluation.ClusterEvaluator;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.RandomGenerator;
import org.corant.modules.dsa.cluster.distance.Distance;

/**
 * corant-modules-dsa
 *
 * @author bingo 上午10:07:45
 */
public class Clusterers {

  public static List<Cluster<DoublePointRecord>> dbsanCluster(
      final Collection<DoublePointRecord> records, final double eps, final int minPts) {
    return new DBSCANClusterer<DoublePointRecord>(eps, minPts).cluster(records);
  }

  public static List<Cluster<DoublePointRecord>> dbsanCluster(
      final Collection<DoublePointRecord> records, final double eps, final int minPts,
      final DistanceMeasure measure) {
    return new DBSCANClusterer<DoublePointRecord>(eps, minPts, measure).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> fuzzyKmeansCluster(
      final Collection<DoublePointRecord> records, final int k, final double fuzziness) {
    return new FuzzyKMeansClusterer<DoublePointRecord>(k, fuzziness).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> fuzzyKmeansCluster(
      final Collection<DoublePointRecord> records, final int k, final double fuzziness,
      final int maxIterations, final DistanceMeasure measure) {
    return new FuzzyKMeansClusterer<DoublePointRecord>(k, fuzziness, maxIterations, measure)
        .cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> fuzzyKmeansCluster(
      final Collection<DoublePointRecord> records, final int k, final double fuzziness,
      final int maxIterations, final DistanceMeasure measure, final double epsilon,
      final RandomGenerator random) {
    return new FuzzyKMeansClusterer<DoublePointRecord>(k, fuzziness, maxIterations, measure,
        epsilon, random).cluster(records);
  }

  public static Map<Centroid, List<MapRecord>> kmeansCluster(List<MapRecord> mapRecords, int k,
      Distance distance, int maxIterations) {
    return DefaultKMeanClusterer.fit(mapRecords, k, distance, maxIterations);
  }

  public static List<CentroidCluster<DoublePointRecord>> kmeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k) {
    return new KMeansPlusPlusClusterer<DoublePointRecord>(k).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> kmeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations) {
    return new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> kmeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure) {
    return new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations, measure)
        .cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> kmeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure, final RandomGenerator random) {
    return new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations, measure, random)
        .cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> kmeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure, final RandomGenerator random,
      final EmptyClusterStrategy emptyStrategy) {
    return new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations, measure, random,
        emptyStrategy).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<DoublePointRecord>(new KMeansPlusPlusClusterer<>(k),
        numTrials).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure, final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<>(
        new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations, measure), numTrials)
            .cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure, final RandomGenerator random,
      final EmptyClusterStrategy emptyStrategy, final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<>(new KMeansPlusPlusClusterer<DoublePointRecord>(k,
        maxIterations, measure, random, emptyStrategy), numTrials).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final DistanceMeasure measure, final RandomGenerator random, final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<>(
        new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations, measure, random),
        numTrials).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records, final int k, final int maxIterations,
      final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<>(
        new KMeansPlusPlusClusterer<DoublePointRecord>(k, maxIterations), numTrials)
            .cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records,
      final KMeansPlusPlusClusterer<DoublePointRecord> clusterer, final int numTrials) {
    return new MultiKMeansPlusPlusClusterer<>(clusterer, numTrials).cluster(records);
  }

  public static List<CentroidCluster<DoublePointRecord>> multiKMeansPlusPlusCluster(
      final Collection<DoublePointRecord> records,
      final KMeansPlusPlusClusterer<DoublePointRecord> clusterer, final int numTrials,
      final ClusterEvaluator<DoublePointRecord> evaluator) {
    return new MultiKMeansPlusPlusClusterer<>(clusterer, numTrials, evaluator).cluster(records);
  }
}
