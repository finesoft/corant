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
package org.corant.modules.jms.marshaller;

import static org.corant.modules.jms.JMSNames.MSG_MARSHAL_SCHEMA_ZIP_BINARY;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Compressors.compress;
import static org.corant.shared.util.Compressors.decompress;
import static org.corant.shared.util.Iterables.iterableOf;
import static org.corant.shared.util.Streams.copy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageFormatRuntimeException;
import jakarta.jms.Session;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.InputStreamResource;

/**
 * corant-modules-jms-api
 *
 * @author bingo 上午11:36:52
 *
 */
@ApplicationScoped
@Named(MSG_MARSHAL_SCHEMA_ZIP_BINARY)
public class ZipBinaryMessageMarshaller implements MessageMarshaller {
  @SuppressWarnings("unchecked")
  @Override
  public <T> T deserialize(Message message, Class<T> clazz) {
    shouldBeTrue(InputStreamResource.class.isAssignableFrom(clazz));
    BytesMessage bmsg = shouldInstanceOf(message, BytesMessage.class);
    try {
      byte[] data = new byte[(int) bmsg.getBodyLength()];
      bmsg.readBytes(data);
      Map<String, Object> metas = new HashMap<>();
      for (Object n : iterableOf(bmsg.getPropertyNames())) {
        metas.put(n.toString(), bmsg.getObjectProperty(n.toString()));
      }
      return (T) new InputStreamResource(metas, new ByteArrayInputStream(decompress(data)));
    } catch (JMSException | IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public Message serialize(JMSContext jmsContext, Object object) {
    return doSerialize(jmsContext.createBytesMessage(),
        shouldInstanceOf(object, InputStream.class));
  }

  @Override
  public Message serialize(Session session, Object object) {
    try {
      return doSerialize(session.createBytesMessage(), object);
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected Message doSerialize(BytesMessage message, Object object) {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(shouldInstanceOf(object, InputStream.class), buffer);
      byte[] bytes = compress(buffer.toByteArray());
      message.writeBytes(bytes);
      return resolveSchemaProperty(message, MSG_MARSHAL_SCHEMA_ZIP_BINARY);
    } catch (JMSException | IOException e) {
      throw new MessageFormatRuntimeException(e.getMessage());
    }
  }
}
