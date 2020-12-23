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
package org.corant.suites.query.shared.dynamic.calcite;

/**
 * corant-suites-query
 *
 * Unfinish yet!
 *
 * @author bingo 下午8:49:44
 *
 */
public class CalciteConnectionPools {

  /*
   * static final Map<String, BasicDataSource> CPS = new ConcurrentHashMap<>();
   *
   * public static DataSource getDataSource(String name, Supplier<CalciteConnectionPoolSetting>
   * supplier) { return CPS.computeIfAbsent(name, (k) -> { BasicDataSource bds = new
   * BasicDataSource(); bds.setRollbackOnReturn(false); // bds.setUrl(Driver.CONNECT_STRING_PREFIX);
   * // bds.setDriver(new Driver()); bds.setInitialSize(1); bds.setJmxName("asosat-calcite-ds-" +
   * name); configPooledDataSource(bds, supplier.get()); return bds; }); }
   *
   * static void configPooledDataSource(BasicDataSource bds, CalciteConnectionPoolSetting setting) {
   * if (setting != null) { Properties pops = setting.getProperties(); if (pops != null) {
   * pops.forEach((pk, pv) -> bds.addConnectionProperty(pk.toString(), pv == null ? null :
   * pv.toString())); } bds.setMaxConnLifetimeMillis(setting.getMaxConnLifetimeMillis());
   * bds.setMaxIdle(setting.getMaxIdle());
   * bds.setMaxOpenPreparedStatements(setting.getMaxOpenPreparedStatements());
   * bds.setMaxTotal(setting.getMaxTotal()); bds.setMaxWaitMillis(setting.getMaxWaitMillis());
   * bds.setMinEvictableIdleTimeMillis(setting.getMinEvictableIdleTimeMillis());
   * bds.setMinIdle(setting.getMinIdle());
   * bds.setNumTestsPerEvictionRun(setting.getNumTestsPerEvictionRun());
   * bds.setSoftMinEvictableIdleTimeMillis(setting.getSoftMinEvictableIdleTimeMillis());
   * bds.setTestOnBorrow(setting.isTestOnBorrow()); bds.setTestOnCreate(setting.isTestOnCreate());
   * bds.setTestOnReturn(setting.isTestOnReturn()); bds.setTestWhileIdle(setting.isTestWhileIdle());
   * bds.setTimeBetweenEvictionRunsMillis(setting.getTimeBetweenEvictionRunsMillis());
   * bds.setValidationQueryTimeout(setting.getValidationQueryTimeout()); if
   * (setting.getDefaultQueryTimeout() != null) {
   * bds.setDefaultQueryTimeout(setting.getDefaultQueryTimeout()); } } }
   *
   * public static class CalciteConnectionPoolSetting { long maxConnLifetimeMillis = -1; int maxIdle
   * = GenericObjectPoolConfig.DEFAULT_MAX_IDLE; int maxOpenPreparedStatements =
   * GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL; int maxTotal =
   * GenericObjectPoolConfig.DEFAULT_MAX_TOTAL; long maxWaitMillis =
   * BaseObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS; long minEvictableIdleTimeMillis =
   * BaseObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS; int minIdle =
   * GenericObjectPoolConfig.DEFAULT_MIN_IDLE; int numTestsPerEvictionRun =
   * BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN; long softMinEvictableIdleTimeMillis =
   * BaseObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS; long
   * timeBetweenEvictionRunsMillis = BaseObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
   * int validationQueryTimeout = -1; boolean testOnBorrow = true; boolean testOnCreate = false;
   * boolean testOnReturn = false; boolean testWhileIdle = false; Properties properties; Integer
   * defaultQueryTimeout = null;
   *
   * public CalciteConnectionPoolSetting() {}
   *
   * public CalciteConnectionPoolSetting(Properties properties) { setProperties(properties); }
   *
   * public Integer getDefaultQueryTimeout() { return defaultQueryTimeout; }
   *
   * public long getMaxConnLifetimeMillis() { return maxConnLifetimeMillis; }
   *
   * public int getMaxIdle() { return maxIdle; }
   *
   * public int getMaxOpenPreparedStatements() { return maxOpenPreparedStatements; }
   *
   * public int getMaxTotal() { return maxTotal; }
   *
   * public long getMaxWaitMillis() { return maxWaitMillis; }
   *
   * public long getMinEvictableIdleTimeMillis() { return minEvictableIdleTimeMillis; }
   *
   * public int getMinIdle() { return minIdle; }
   *
   * public int getNumTestsPerEvictionRun() { return numTestsPerEvictionRun; }
   *
   * public Properties getProperties() { return properties; }
   *
   * public long getSoftMinEvictableIdleTimeMillis() { return softMinEvictableIdleTimeMillis; }
   *
   * public long getTimeBetweenEvictionRunsMillis() { return timeBetweenEvictionRunsMillis; }
   *
   * public int getValidationQueryTimeout() { return validationQueryTimeout; }
   *
   * public boolean isTestOnBorrow() { return testOnBorrow; }
   *
   * public boolean isTestOnCreate() { return testOnCreate; }
   *
   * public boolean isTestOnReturn() { return testOnReturn; }
   *
   * public boolean isTestWhileIdle() { return testWhileIdle; }
   *
   * public void setDefaultQueryTimeout(Integer defaultQueryTimeout) { this.defaultQueryTimeout =
   * defaultQueryTimeout; }
   *
   * public void setMaxConnLifetimeMillis(long maxConnLifetimeMillis) { this.maxConnLifetimeMillis =
   * maxConnLifetimeMillis; }
   *
   * public void setMaxIdle(int maxIdle) { this.maxIdle = maxIdle; }
   *
   * public void setMaxOpenPreparedStatements(int maxOpenPreparedStatements) {
   * this.maxOpenPreparedStatements = maxOpenPreparedStatements; }
   *
   * public void setMaxTotal(int maxTotal) { this.maxTotal = maxTotal; }
   *
   * public void setMaxWaitMillis(long maxWaitMillis) { this.maxWaitMillis = maxWaitMillis; }
   *
   * public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
   * this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis; }
   *
   * public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
   *
   * public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) { this.numTestsPerEvictionRun
   * = numTestsPerEvictionRun; }
   *
   * public void setProperties(Properties properties) { this.properties = properties; }
   *
   * public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
   * this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis; }
   *
   * public void setTestOnBorrow(boolean testOnBorrow) { this.testOnBorrow = testOnBorrow; }
   *
   * public void setTestOnCreate(boolean testOnCreate) { this.testOnCreate = testOnCreate; }
   *
   * public void setTestOnReturn(boolean testOnReturn) { this.testOnReturn = testOnReturn; }
   *
   * public void setTestWhileIdle(boolean testWhileIdle) { this.testWhileIdle = testWhileIdle; }
   *
   * public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
   * this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis; }
   *
   * public void setValidationQueryTimeout(int validationQueryTimeout) { this.validationQueryTimeout
   * = validationQueryTimeout; }
   *
   * }
   */
}
