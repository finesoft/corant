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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.suites.dsa.cluster.distance.Distance;

/**
 * corant-suites-dsa
 *
 * Code base from https://github.com/eugenp/tutorials/blob/master/algorithms-miscellaneous-3
 *
 * @author bingo 下午2:14:34
 *
 */
public class DefaultKMeanClusterer {
  /**
   * Will be used to generate random numbers.
   */
  private static final Random random = new Random();

  /**
   * Performs the K-Means clustering algorithm on the given dataset.
   *
   * @param mapRecords The dataset.
   * @param k Number of Clusters.
   * @param distance To calculate the distance between two items.
   * @param maxIterations Upper bound for the number of iterations.
   * @return K clusters along with their features.
   */
  public static Map<Centroid, List<MapRecord>> fit(List<MapRecord> mapRecords, int k,
      Distance distance, int maxIterations) {
    applyPreconditions(mapRecords, k, distance, maxIterations);

    List<Centroid> centroids = randomCentroids(mapRecords, k);
    Map<Centroid, List<MapRecord>> clusters = new HashMap<>();
    Map<Centroid, List<MapRecord>> lastState = new HashMap<>();

    // iterate for a pre-defined number of times
    for (int i = 0; i < maxIterations; i++) {
      boolean isLastIteration = i == maxIterations - 1;

      // in each iteration we should find the nearest centroid for each record
      for (MapRecord mapRecord : mapRecords) {
        Centroid centroid = nearestCentroid(mapRecord, centroids, distance);
        assignToCluster(clusters, mapRecord, centroid);
      }

      // if the assignment does not change, then the algorithm terminates
      boolean shouldTerminate = isLastIteration || clusters.equals(lastState);
      lastState = clusters;
      if (shouldTerminate) {
        break;
      }

      // at the end of each iteration we should relocate the centroids
      centroids = relocateCentroids(clusters);
      clusters = new HashMap<>();
    }

    return lastState;
  }

  private static void applyPreconditions(List<MapRecord> mapRecords, int k, Distance distance,
      int maxIterations) {
    if (mapRecords == null || mapRecords.isEmpty()) {
      throw new IllegalArgumentException("The dataset can't be empty");
    }

    if (k <= 1) {
      throw new IllegalArgumentException(
          "It doesn't make sense to have less than or equal to 1 cluster");
    }

    if (distance == null) {
      throw new IllegalArgumentException("The distance calculator is required");
    }

    if (maxIterations <= 0) {
      throw new IllegalArgumentException("Max iterations should be a positive number");
    }
  }

  /**
   * Assigns a feature vector to the given centroid. If this is the first assignment for this
   * centroid, first we should create the list.
   *
   * @param clusters The current cluster configuration.
   * @param mapRecord The feature vector.
   * @param centroid The centroid.
   */
  private static void assignToCluster(Map<Centroid, List<MapRecord>> clusters, MapRecord mapRecord,
      Centroid centroid) {
    clusters.compute(centroid, (key, list) -> {
      if (list == null) {
        list = new ArrayList<>();
      }

      list.add(mapRecord);
      return list;
    });
  }

  /**
   * Moves the given centroid to the average position of all assigned features. If the centroid has
   * no feature in its cluster, then there would be no need for a relocation. Otherwise, for each
   * entry we calculate the average of all records first by summing all the entries and then
   * dividing the final summation value by the number of records.
   *
   * @param centroid The centroid to move.
   * @param mapRecords The assigned features.
   * @return The moved centroid.
   */
  private static Centroid average(Centroid centroid, List<MapRecord> mapRecords) {
    // if this cluster is empty, then we shouldn't move the centroid
    if (mapRecords == null || mapRecords.isEmpty()) {
      return centroid;
    }

    // Since some records don't have all possible attributes, we initialize
    // average coordinates equal to current centroid coordinates
    Map<Object, Double> average = centroid.getCoordinates();

    // The average function works correctly if we clear all coordinates corresponding
    // to present record attributes
    mapRecords.stream().flatMap(e -> e.getFeatures().keySet().stream())
        .forEach(k -> average.put(k, 0.0));

    for (MapRecord mapRecord : mapRecords) {
      mapRecord.getFeatures()
          .forEach((k, v) -> average.compute(k, (k1, currentValue) -> v + currentValue));
    }

    average.forEach((k, v) -> average.put(k, v / mapRecords.size()));

    return new Centroid(average);
  }

  /**
   * With the help of the given distance calculator, iterates through centroids and finds the
   * nearest one to the given record.
   *
   * @param mapRecord The feature vector to find a centroid for.
   * @param centroids Collection of all centroids.
   * @param distance To calculate the distance between two items.
   * @return The nearest centroid to the given feature vector.
   */
  private static Centroid nearestCentroid(MapRecord mapRecord, List<Centroid> centroids,
      Distance distance) {
    double minimumDistance = Double.MAX_VALUE;
    Centroid nearest = null;

    for (Centroid centroid : centroids) {
      double currentDistance =
          distance.calculate(mapRecord.getFeatures(), centroid.getCoordinates());
      if (currentDistance < minimumDistance) {
        minimumDistance = currentDistance;
        nearest = centroid;
      }
    }

    return nearest;
  }

  /**
   * Generates k random centroids. Before kicking-off the centroid generation process, first we
   * calculate the possible value range for each attribute. Then when we're going to generate the
   * centroids, we generate random coordinates in the [min, max] range for each attribute.
   *
   * @param mapRecords The dataset which helps to calculate the [min, max] range for each attribute.
   * @param k Number of clusters.
   * @return Collections of randomly generated centroids.
   */
  private static List<Centroid> randomCentroids(List<MapRecord> mapRecords, int k) {
    List<Centroid> centroids = new ArrayList<>();
    Map<Object, Double> maxs = new HashMap<>();
    Map<Object, Double> mins = new HashMap<>();

    for (MapRecord mapRecord : mapRecords) {
      mapRecord.getFeatures().forEach((key, value) -> {
        // compares the value with the current max and choose the bigger value between them
        maxs.compute(key, (k1, max) -> max == null || value > max ? value : max);

        // compare the value with the current min and choose the smaller value between them
        mins.compute(key, (k1, min) -> min == null || value < min ? value : min);
      });
    }

    Set<Object> attributes = mapRecords.stream().flatMap(e -> e.getFeatures().keySet().stream())
        .collect(Collectors.toSet());
    for (int i = 0; i < k; i++) {
      Map<Object, Double> coordinates = new HashMap<>();
      for (Object attribute : attributes) {
        double max = maxs.get(attribute);
        double min = mins.get(attribute);
        coordinates.put(attribute, random.nextDouble() * (max - min) + min);
      }
      centroids.add(new Centroid(coordinates));
    }

    return centroids;
  }

  /**
   * Move all cluster centroids to the average of all assigned features.
   *
   * @param clusters The current cluster configuration.
   * @return Collection of new and relocated centroids.
   */
  private static List<Centroid> relocateCentroids(Map<Centroid, List<MapRecord>> clusters) {
    return clusters.entrySet().stream().map(e -> average(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }
}
