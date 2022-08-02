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
package org.corant.shared.util;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.newHashMap;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * A simple tool class use for register, de-register or invoke MBean operations.
 *
 * @author bingo 12:00:58
 *
 */
public class MBeans {

  /**
   * Deregisters the given object instances from MBean server if it is already registered.
   *
   * @param objectInstances the object instances to be deregistered.
   */
  public static void deregisterFromMBean(ObjectInstance... objectInstances) {
    if (isEmpty(objectInstances)) {
      return;
    }
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (ObjectInstance objectInstance : objectInstances) {
      if (objectInstance != null && objectInstance.getObjectName() != null) {
        try {
          if (server.isRegistered(objectInstance.getObjectName())) {
            server.unregisterMBean(objectInstance.getObjectName());
          }
        } catch (MBeanRegistrationException | InstanceNotFoundException ex) {
          throw new CorantRuntimeException(ex);
        }
      }
    }
  }

  /**
   * Deregisters the given object name from MBean server if it is already registered.
   *
   * @param names the object names to be deregistered.
   */
  public static void deregisterFromMBean(String... names) {
    if (isEmpty(names)) {
      return;
    }
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (String name : names) {
      if (isNotBlank(name)) {
        ObjectName objectName;
        try {
          objectName = new ObjectName(name);
        } catch (MalformedObjectNameException ex) {
          throw new CorantRuntimeException(ex);
        }
        try {
          if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName);
          }
        } catch (MBeanRegistrationException | InstanceNotFoundException ex) {
          throw new CorantRuntimeException(ex);
        }
      }
    }
  }

  /**
   * Returns true if the given object name is already registered with the MBean server, false
   * otherwise.
   *
   * @param name the object name to be checked
   * @return true if the given object name is already registered otherwise false.
   */
  public static boolean isRegistered(String name) {
    if (isBlank(name)) {
      return false;
    }
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      return server.isRegistered(new ObjectName(name));
    } catch (MalformedObjectNameException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  /**
   * Returns a remote MBInvoker.
   *
   * @param <T> the management interface type
   * @param clazz the management interface that the MBean exports, which will also be implemented by
   *        the inner proxy for invoking.
   * @param notificationBroadcaster make the inner proxy implement NotificationEmitter by forwarding
   *        its methods via connection.
   * @return a simple invoker builder.
   *
   * @see MBInvoker
   * @see MBInvoker#invoke(Consumer)
   * @see MBInvoker#invoke(Function)
   */
  public static <T> MBInvoker<T> newInvoker(Class<T> clazz, boolean notificationBroadcaster) {
    return new MBInvoker<>(clazz, notificationBroadcaster);
  }

  /**
   * Register the given object to MBean server by the given object name and return the MBean object
   * instance.
   * <p>
   * Note: if the given name was already registered with MBean server, then this method will
   * de-register the exists MBean with same name and then register the given object to MBean server
   * by the name.
   *
   * @param name the object name to be registered
   * @param object the object to be registered
   * @return the MBean object instance
   */
  public static ObjectInstance registerToMBean(String name, Object object) {
    return registerToMBean(name, object, true);
  }

  /**
   * Register to the MBean server, may re-register if necessary.
   *
   * @param name the object name to be registered
   * @param object the object to be registered
   * @param reregister If the given name is already registered with the MBean server, if the
   *        parameter is false, it will return the registered object instance with the same name,
   *        otherwise the registered object with the same name will be deregistered from the MBean
   *        server, and then the given object will be registered with the MBean server by the given
   *        name and return the object instance.
   * @return the MBean object instance
   */
  public static ObjectInstance registerToMBean(String name, Object object, boolean reregister) {
    if (object == null || isBlank(name)) {
      return null;
    }
    ObjectName objectName;
    try {
      objectName = new ObjectName(name);
    } catch (MalformedObjectNameException ex) {
      throw new CorantRuntimeException(ex);
    }

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      if (server.isRegistered(objectName)) {
        if (!reregister) {
          return server.getObjectInstance(objectName);
        } else {
          server.unregisterMBean(objectName);
        }
      }
      return server.registerMBean(object, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException
        | NotCompliantMBeanException | InstanceNotFoundException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  /**
   * corant-shared
   *
   * <p>
   * A simple MBean remote invocation tool class.
   *
   * <pre>
   * <b>The following shows how to invoke ActiveMQServerControl with JMX RMI:</b>
   * public void showJMSConnections() {
   *   System.setProperty("java.rmi.server.hostname", "localhost");
   *   final String domain = ActiveMQDefaultConfiguration.getDefaultJmxDomain();
   *   final ObjectNameBuilder objectNameBuilder = ObjectNameBuilder.create(domain, "0.0.0.0", true);
   *   final ObjectName objectName = objectNameBuilder.getActiveMQServerObjectName();
   *   final String url = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
   *   final Map<String, ?> environment = mapOf(JMXConnector.CREDENTIALS, new String[] {"**", "**"});
   *   new MBInvoker<>(ActiveMQServerControl.class, false).objectName(objectName).environment(environment).url(url)
   *   .invoke(activeMQServerControl -> {
   *           try {
   *             System.out.println(activeMQServerControl.listConnectionsAsJSON());
   *           } catch (Exception e) {
   *             throw new CorantRuntimeException(e);
   *           }
   *         });
   * }
   * </pre>
   *
   *
   * @author bingo 下午10:36:21
   *
   */
  public static class MBInvoker<T> {
    JMXServiceURL url;
    ObjectName objectName;
    final boolean notificationBroadcaster;
    final Class<T> clazz;
    Map<String, ?> environment;

    /**
     * Create a MBInvoker
     *
     * @param clazz the management interface that the MBean exports, which will also be implemented
     *        by the inner proxy for invoking.
     * @param notificationBroadcaster make the inner proxy implement NotificationEmitter by
     *        forwarding its methods via connection.
     *
     * @see MBeanServerInvocationHandler#newProxyInstance(MBeanServerConnection, ObjectName, Class,
     *      boolean)
     */
    public MBInvoker(Class<T> clazz, boolean notificationBroadcaster) {
      this.clazz = clazz;
      this.notificationBroadcaster = notificationBroadcaster;
    }

    /**
     * Set the environment for JMXConnector creating. The environment is a set of attributes to
     * determine how the connection is made, such as principal credential etc. This parameter can be
     * null. Keys in this map must be Strings. The appropriate type of each associated value depends
     * on the attribute. The contents of environment are not changed by this call.
     *
     * @param environment the environment for JMXConnector creating
     * @return environment
     */
    public MBInvoker<T> environment(Map<String, ?> environment) {
      if (environment != null) {
        this.environment = newHashMap(environment);
      }
      return this;
    }

    /**
     * Invoke remote MBean with given consumer
     *
     * @param consumer the consumer that accepts MBean instance for invoking
     */
    public void invoke(Consumer<T> consumer) {
      try (JMXConnector connector = JMXConnectorFactory.connect(url, environment)) {
        MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();
        T bean = MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName,
            clazz, notificationBroadcaster);
        consumer.accept(bean);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }

    /**
     * Invoke remote MBean with given function
     *
     * @param function the function that accepts MBean instance and return result for invoking
     */
    public <R> R invoke(Function<T, R> function) {
      try (JMXConnector connector = JMXConnectorFactory.connect(url, environment)) {
        MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();
        T bean = MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName,
            clazz, notificationBroadcaster);
        return function.apply(bean);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }

    /**
     * Set the object name of the MBean within connection to forward to.
     *
     * @param objectName the name of the MBean within connection to forward to.
     */
    public MBInvoker<T> objectName(ObjectName objectName) {
      this.objectName = shouldNotNull(objectName);
      return this;
    }

    /**
     * Set the object name of the MBean within connection to forward to.
     *
     * @param objectName the name of the MBean within connection to forward to.
     */
    public MBInvoker<T> objectName(String objectName) {
      try {
        this.objectName = new ObjectName(objectName);
      } catch (MalformedObjectNameException e) {
        throw new CorantRuntimeException(e);
      }
      return this;
    }

    /**
     * Set the jmx rmi url string for invoker to create JMXConnector
     *
     * @param url the jmx rmi url
     */
    public MBInvoker<T> url(JMXServiceURL url) {
      this.url = shouldNotNull(url);
      return this;
    }

    /**
     * Set the jmx rmi url string for invoker to create JMXConnector
     * <p>
     * Example url: service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi
     *
     * @param url the jmx rmi url string for JMXConnector creating.
     */
    public MBInvoker<T> url(String url) {
      try {
        this.url = new JMXServiceURL(shouldNotBlank(url));
      } catch (MalformedURLException e) {
        throw new CorantRuntimeException(e);
      }
      return this;
    }
  }
}
