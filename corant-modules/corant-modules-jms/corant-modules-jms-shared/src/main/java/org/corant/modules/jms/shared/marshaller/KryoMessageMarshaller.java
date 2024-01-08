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
package org.corant.modules.jms.shared.marshaller;

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Classes.getUserClass;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.shared.exception.CorantRuntimeException;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午11:34:02
 */
@ApplicationScoped
@Named("KRYO")
public class KryoMessageMarshaller implements MessageMarshaller {

  protected static final Map<Class<?>, Serializer<?>> customSerializers = new ConcurrentHashMap<>();

  protected static final ThreadLocal<Kryo> kryoCache = ThreadLocal.withInitial(() -> {
    Kryo inst = new Kryo();
    customSerializers.forEach(inst::addDefaultSerializer);
    inst.setRegistrationRequired(false);// FIXME
    inst.setReferences(false);
    return inst;
  });

  public static Serializer<?> putCustomSerializer(Class<?> type, Serializer<?> serializer) {
    return customSerializers.put(type, serializer);
  }

  public static Serializer<?> removeCustomSerializer(Class<?> type) {
    return customSerializers.remove(type);
  }

  @Override
  public <T> T deserialize(Message message, Class<T> clazz) {
    BytesMessage bytMsg = shouldInstanceOf(message, BytesMessage.class);
    try {
      if (bytMsg.getBodyLength() == 0) {
        return null;
      }
      byte[] data = new byte[(int) bytMsg.getBodyLength()];
      bytMsg.readBytes(data);
      return fromBytes(data, clazz);
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public Message serialize(JMSContext jmsContext, Object object) {
    BytesMessage bytMsg = jmsContext.createBytesMessage();
    if (object != null) {
      try {
        bytMsg.writeBytes(toBytes(object));
      } catch (JMSException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return resolveSchemaProperty(bytMsg, "KRYO");
  }

  @Override
  public Message serialize(Session session, Object object) {
    try {
      BytesMessage bytMsg = session.createBytesMessage();
      if (object != null) {
        bytMsg.writeBytes(toBytes(object));
      }
      return resolveSchemaProperty(bytMsg, "KRYO");
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected <T> T fromBytes(byte[] bytes, Class<T> clazz) {
    final Kryo kryo = kryoCache.get();
    kryo.register(getUserClass(clazz));
    try (Input in = new Input(bytes)) {
      return kryo.readObject(in, clazz);
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    customSerializers.clear();
  }

  protected byte[] toBytes(Object object) {
    final Kryo kryo = kryoCache.get();
    kryo.register(getUserClass(object));
    try (Output output = new Output(new ByteArrayOutputStream())) {
      kryo.writeObject(output, object);
      return output.getBuffer();
    }
  }
}
