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
package org.corant.suites.mongodb;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.MapUtils.asMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources.ClassPathResource;
import org.corant.shared.util.Resources.FileSystemResource;
import org.corant.shared.util.Resources.Resource;
import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午4:19:59
 *
 */
@ApplicationScoped
public abstract class AbstractGridFSBucketProvider {

  public static final int DFLT_CHUNK_SIZE_BYTES = 256;

  protected abstract GridFSBucket getBucket();

  protected GridFSDownloadStream getFile(Serializable id) {
    try {
      return getBucket().openDownloadStream(MongoClientExtension.bsonId(id));
    } catch (MongoGridFSException e) {
      return null;
    }
  }

  protected void putFile(Serializable id, Resource r) {
    try (InputStream is = r.openStream()) {
      if (r instanceof ClassPathResource) {
        ClassPathResource cpr = ClassPathResource.class.cast(r);
        putFile(id, cpr.getResourceName(), DFLT_CHUNK_SIZE_BYTES, is,
            asMap("sourceType", cpr.getSourceType(), "location", cpr.getLocation()));
      } else if (r instanceof FileSystemResource) {
        FileSystemResource fsr = FileSystemResource.class.cast(r);
        putFile(id, fsr.getFile().getName(), DFLT_CHUNK_SIZE_BYTES, is,
            asMap("sourceType", fsr.getSourceType(), "length", fsr.getFile().length(),
                "lastModified", fsr.getFile().lastModified(), "path", fsr.getFile().getPath()));
      } else {
        putFile(id, r.getLocation(), DFLT_CHUNK_SIZE_BYTES, is,
            asMap("sourceType", r.getSourceType(), "location", r.getLocation()));
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected void putFile(Serializable id, String filename, int chunkSizeBytes, InputStream is,
      Map<String, Object> metadata) {
    getBucket().uploadFromStream(MongoClientExtension.bsonId(id), filename, shouldNotNull(is),
        new GridFSUploadOptions().chunkSizeBytes(chunkSizeBytes));
  }

  protected void removeFile(Serializable id) {
    getBucket().delete(MongoClientExtension.bsonId(id));
  }
}
