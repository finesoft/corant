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
/**
 * corant-suites-jms-artemis
 *
 * A JMSContext is the main interface in the simplified JMS API introduced for JMS 2.0. This
 * combines in a single object the functionality of two separate objects from the JMS 1.1 API: a
 * Connection and a Session. When an application needs to send messages it use the createProducer
 * method to create a JMSProducer which provides methods to configure and send messages. Messages
 * may be sent either synchronously or asynchronously.
 * 
 * When an application needs to receive messages it uses one of several createConsumer or
 * createDurableConsumer methods to create a JMSConsumer . A JMSConsumer provides methods to receive
 * messages either synchronously or asynchronously.
 * 
 * In terms of the JMS 1.1 API a JMSContext should be thought of as representing both a Connection
 * and a Session. Although the simplified API removes the need for applications to use those
 * objects, the concepts of connection and session remain important. A connection represents a
 * physical link to the JMS server and a session represents a single-threaded context for sending
 * and receiving messages.
 * 
 * A JMSContext may be created by calling one of several createContext methods on a
 * ConnectionFactory. A JMSContext that is created in this way is described as being
 * application-managed. An application-managed JMSContext must be closed when no longer needed by
 * calling its close method.
 * 
 * Applications running in the Java EE web and EJB containers may alternatively inject a JMSContext
 * into their application using the @Inject annotation. A JMSContext that is created in this way is
 * described as being container-managed. A container-managed JMSContext will be closed automatically
 * by the container.
 * 
 * Applications running in the Java EE web and EJB containers are not permitted to create more than
 * one active session on a connection so combining them in a single object takes advantage of this
 * restriction to offer a simpler API.
 * 
 * However applications running in a Java SE environment or in the Java EE application client
 * container are permitted to create multiple active sessions on the same connection. This allows
 * the same physical connection to be used in multiple threads simultaneously. Such applications
 * which require multiple sessions to be created on the same connection should use one of the
 * createContext methods on the ConnectionFactory to create the first JMSContext and then use the
 * createContext method on JMSContext to create additional JMSContext objects that use the same
 * connection. All these JMSContext objects are application-managed and must be closed when no
 * longer needed by calling their close method.
 *
 * @author bingo 下午4:34:24
 *
 */
package org.corant.suites.jms.artemis;
