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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * Simple tool classes are used for MBean-related registration and cancellation operations.
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
}
