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

  public static void deregisterFromMBean(String... objectNamings) {
    if (isEmpty(objectNamings)) {
      return;
    }
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (String objectNaming : objectNamings) {
      if (isNotBlank(objectNaming)) {
        ObjectName objectName = null;
        try {
          objectName = new ObjectName(objectNaming);
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

  public static boolean isRegistered(String objectNaming) throws MBeanRegistrationException {
    if (isBlank(objectNaming)) {
      return false;
    }
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      return server.isRegistered(new ObjectName(objectNaming));
    } catch (MalformedObjectNameException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static ObjectInstance registerToMBean(String objectNaming, Object object) {
    return registerToMBean(objectNaming, object, true);
  }

  /**
   * Register to the MBean server, can re-register if necessary.
   *
   * @param objectNaming
   * @param object
   * @param reregister If the object has already registered with the object naming, if this
   *        parameter is true then return the registered object instance else the object will be
   *        unregistered and then re-register and return the object instance.
   * @return registerToMBean
   */
  public static ObjectInstance registerToMBean(String objectNaming, Object object,
      boolean reregister) {
    if (object == null || isBlank(objectNaming)) {
      return null;
    }
    ObjectName objectName = null;
    try {
      objectName = new ObjectName(objectNaming);
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
