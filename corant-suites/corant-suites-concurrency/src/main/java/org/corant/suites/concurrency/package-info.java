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
 * corant-suites-concurrency
 *
 * Java EE depends on various context information to be available on the thread when interacting
 * with other Java EE services such as JDBC data sources, JMS providers and EJBs. When using Java EE
 * services from a noncontainer thread, the following behaviors are required:
 * <li>Saving the application component thread’s container context.</li> 
 * <li>Identifying which container contexts to save and propagate.</li> 
 * <li>Applying a container context to the current thread.</li> 
 * <li>Restoring a thread's original context.</li>
 *
 * <p>
 * The types of contexts to be propagated from a contextualizing application component include JNDI
 * naming context, classloader, and security information. containers can choose to support
 * propagation of other types of context.
 *
 * <p>
 * This subsection describes additional requirements for ManagedExecutorService providers.
 * <p>
 * 1. All tasks, when executed from the ManagedExecutorService, will run with the Java EE component
 * identity of the component that submitted the task.
 * <p>
 * 2. The lifecycle of a ManagedExecutorService is managed by an application server. All lifecycle
 * operations on the ManagedExecutorService interface will throw a java.lang.IllegalStateException
 * exception. This includes the following methods that are defined in the
 * java.util.concurrent.ExecutorService interface: awaitTermination(), isShutdown(), isTerminated(),
 * shutdown(), and shutdownNow().
 * <p>
 * 3. No task submitted to an executor can run if task’s component is not started. When a
 * ManagedExecutorService instance is being shutdown by the Java EE Product Provider:
 * <p>
 * 1. All attempts to submit new tasks are rejected.
 * <p>
 * 2. All submitted tasks are cancelled if not running.
 * <p>
 * 3. All running task threads are interrupted.
 * <p>
 * 4. All registered ManagedTaskListeners are invoked.
 *
 * <p>
 * Must support user-managed global transaction demarcation using the
 * javax.transaction.UserTransaction interface. Task instances are run outside of the scope of the
 * transaction of the submitting thread. Any transaction active in the executing thread will be
 * suspended.
 *
 * @author bingo 下午6:33:59
 *
 */
package org.corant.suites.concurrency;
