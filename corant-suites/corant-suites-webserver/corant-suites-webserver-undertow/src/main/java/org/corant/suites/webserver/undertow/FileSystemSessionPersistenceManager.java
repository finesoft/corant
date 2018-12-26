/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.webserver.undertow;

import static org.corant.shared.util.ObjectUtils.forceCast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.SerializationUtils.ObjectInputStreamWithLoader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.undertow.servlet.UndertowServletLogger;
import io.undertow.servlet.api.SessionPersistenceManager;

/**
 * corant-suites-webserver-undertow
 *
 * @author bingo 下午3:51:19
 *
 */
@ApplicationScoped
public class FileSystemSessionPersistenceManager implements SessionPersistenceManager {

  @Inject
  @ConfigProperty(name = "webserver.undertow.session-persistence-path")
  private Optional<String> path;

  @Inject
  protected Logger logger;

  @Override
  public void clear(String deploymentName) {
    if (getStore(deploymentName).delete()) {
      logger.info(() -> String.format("Delete %s' session persistence file", deploymentName));
    }
  }

  @Override
  public Map<String, PersistentSession> loadSessionAttributes(String deploymentName,
      ClassLoader classLoader) {
    final File file = getStore(deploymentName);
    if (file.canRead() && file.length() > 0) {
      try (ObjectInputStream stream =
          new ObjectInputStreamWithLoader(new FileInputStream(file), classLoader)) {
        Map<String, SerializablePersistentSession> session = forceCast(stream.readObject());
        long time = System.currentTimeMillis();
        Map<String, PersistentSession> result = new LinkedHashMap<>();
        for (Map.Entry<String, SerializablePersistentSession> entry : session.entrySet()) {
          PersistentSession entrySession = entry.getValue().getPersistentSession();
          if (entrySession.getExpiration().getTime() > time) {
            result.put(entry.getKey(), entrySession);
          }
        }
        return result;
      } catch (IOException | ClassNotFoundException e) {
        UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(e);
      }
    } else {
      logger.info(() -> String.format("Can not find persistence session content from file %s",
          file.getAbsolutePath()));
    }
    return null;
  }

  @Override
  public void persistSessions(String deploymentName, Map<String, PersistentSession> sessionData) {
    try (ObjectOutputStream stream =
        new ObjectOutputStream(new FileOutputStream(getStore(deploymentName)))) {
      Map<String, Serializable> session = new LinkedHashMap<>();
      for (Map.Entry<String, PersistentSession> entry : sessionData.entrySet()) {
        session.put(entry.getKey(), new SerializablePersistentSession(entry.getValue()));
      }
      stream.writeObject(session);
    } catch (IOException e) {
      UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(e);
    }
  }

  protected File getStore(String deploymentName) {
    File file = null;
    if (path.isPresent()) {
      file = new File(path.get(), "session");
    } else {
      file = new File(Paths.get(System.getProperty("user.home"))
          .resolve("." + deploymentName + "-session").resolve("session").toUri());
    }
    final String path = file.getAbsolutePath();
    if (!file.exists()) {
      boolean created = false;
      try {
        if (!file.getParentFile().exists()) {
          if (file.getParentFile().mkdirs()) {
            created = file.createNewFile();
          }
        }
      } catch (IOException e) {
        UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(e);
      }
      if (created) {
        logger.info(() -> String.format("Created session persistence file %s", path));
      } else {
        logger.info(() -> String.format("Can not create session persistence file %s", path));
      }
    }
    logger.info(() -> String.format("Session persistence file is %s", path));
    return file;
  }

  public static class SerializablePersistentSession implements Serializable {

    private static final long serialVersionUID = 0L;

    private final Date expiration;

    private final Map<String, Object> sessionData;

    SerializablePersistentSession(PersistentSession session) {
      expiration = session.getExpiration();
      sessionData = new LinkedHashMap<>(session.getSessionData());
    }

    public PersistentSession getPersistentSession() {
      return new PersistentSession(expiration, sessionData);
    }

  }

}
