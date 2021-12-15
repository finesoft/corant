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

import static org.corant.shared.util.Classes.checkPackageAccess;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午4:26:15
 *
 */
public class Serializations {

  private Serializations() {
  }

  public static Object deserialize(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return ois.readObject();
    } catch (IOException | ClassNotFoundException ex) {
      throw new CorantRuntimeException(ex, "Failed to deserialize object, %s." + ex.getMessage());
    }
  }

  public static byte[] serialize(Object object) {
    if (object == null) {
      return Bytes.EMPTY_ARRAY;
    }
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
      return baos.toByteArray();
    } catch (IOException ex) {
      throw new CorantRuntimeException(ex, "Failed to serialize object of type: %s.",
          object.getClass());
    }
  }

  public static class ObjectInputStreamWithLoader extends ObjectInputStream {

    private final ClassLoader loader;

    public ObjectInputStreamWithLoader(InputStream in, ClassLoader theLoader) throws IOException {
      super(in);
      loader = theLoader;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass aClass)
        throws IOException, ClassNotFoundException {
      if (loader == null) {
        return super.resolveClass(aClass);
      } else {
        String name = aClass.getName();
        checkPackageAccess(name);
        // Query the class loader ...
        return Class.forName(name, false, loader);
      }
    }
  }
}
