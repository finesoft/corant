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
package org.corant.suites.mongodb;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午12:10:04
 *
 */
public class MongoDBConfig {

  public static final int DEFAULT_PORT = 27017;
  public static final String DEFAULT_URI = "mongodb://localhost/test";

  private String host;

  private Integer port = null;

  private String uri;

  private String database;

  private String authenticationDatabase;

  private String gridFsDatabase;

  private String username;

  private char[] password;

  private Class<?> fieldNamingStrategy;

  /**
   *
   * @return the authenticationDatabase
   */
  public String getAuthenticationDatabase() {
    return authenticationDatabase;
  }

  /**
   *
   * @return the database
   */
  public String getDatabase() {
    return database;
  }

  /**
   *
   * @return the fieldNamingStrategy
   */
  public Class<?> getFieldNamingStrategy() {
    return fieldNamingStrategy;
  }

  /**
   *
   * @return the gridFsDatabase
   */
  public String getGridFsDatabase() {
    return gridFsDatabase;
  }

  /**
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   *
   * @return the password
   */
  public char[] getPassword() {
    return password;
  }

  /**
   *
   * @return the port
   */
  public Integer getPort() {
    return port;
  }

  /**
   *
   * @return the uri
   */
  public String getUri() {
    return uri;
  }

  /**
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   *
   * @param authenticationDatabase the authenticationDatabase to set
   */
  protected void setAuthenticationDatabase(String authenticationDatabase) {
    this.authenticationDatabase = authenticationDatabase;
  }

  /**
   *
   * @param database the database to set
   */
  protected void setDatabase(String database) {
    this.database = database;
  }

  /**
   *
   * @param fieldNamingStrategy the fieldNamingStrategy to set
   */
  protected void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
    this.fieldNamingStrategy = fieldNamingStrategy;
  }

  /**
   *
   * @param gridFsDatabase the gridFsDatabase to set
   */
  protected void setGridFsDatabase(String gridFsDatabase) {
    this.gridFsDatabase = gridFsDatabase;
  }

  /**
   *
   * @param host the host to set
   */
  protected void setHost(String host) {
    this.host = host;
  }

  /**
   *
   * @param password the password to set
   */
  protected void setPassword(char[] password) {
    this.password = password;
  }

  /**
   *
   * @param port the port to set
   */
  protected void setPort(Integer port) {
    this.port = port;
  }

  /**
   *
   * @param uri the uri to set
   */
  protected void setUri(String uri) {
    this.uri = uri;
  }

  /**
   *
   * @param username the username to set
   */
  protected void setUsername(String username) {
    this.username = username;
  }

}
