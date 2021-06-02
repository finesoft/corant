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
package org.corant.modules.jms;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:28:21
 *
 */
public interface JMSNames {
  String SECURITY_CONTEXT_PROPERTY_NAME = "__CORANT_SECURITY_CONTEXT__";
  String MSG_MARSHAL_SCHAME = "__CORANT_MSG_MARSHAL_SCHAME__";
  String REPLY_MSG_MARSHAL_SCHAME = "__CORANT_REPLY_MSG_MARSHAL_SCHAME__";
  String MSG_MARSHAL_SCHAME_ZIP_BINARY = "ZIP_BINARY";
  String MSG_MARSHAL_SCHAME_STD_JAVA = "STD_JAVA";
}
