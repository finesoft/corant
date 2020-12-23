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
package org.corant.devops.maven.plugin;

/**
 * corant-devops-maven
 *
 * @author bingo 上午11:23:48
 *
 */
public class BuildStageException extends RuntimeException {

  private static final long serialVersionUID = -1147901031577231763L;

  /**
   *
   */
  public BuildStageException() {
    super();
  }

  /**
   * @param message
   */
  public BuildStageException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public BuildStageException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   */
  public BuildStageException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  /**
   * @param cause
   */
  public BuildStageException(Throwable cause) {
    super(cause);
  }


}
