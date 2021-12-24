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
package org.corant.modules.cloud.alibaba.oss;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.getMapLong;
import static org.corant.shared.util.Maps.getMapZonedDateTime;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.FileUtils;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.StorageClass;

/**
 * corant-modules-cloud-alibaba
 *
 * @author bingo 上午11:31:54
 *
 */
public class OSSStorageService {
  protected final Logger logger = Logger.getLogger(getClass().getName());

  protected String bucketName;
  protected OSS oss;

  protected OSSStorageService() {}

  /**
   * @param bucketName the oss bucket name
   * @param oss the oss to use
   */
  protected OSSStorageService(String bucketName, OSS oss) {
    this.bucketName = bucketName;
    this.oss = oss;
  }

  public Resource get(String id) {
    try {
      final GetObjectRequest request = new GetObjectRequest(bucketName, id);
      final String location = request.getAbsoluteUri().toString();
      final OSSObject object = oss.getObject(request);
      return new OSSResource(object, location);
    } catch (OSSException e) {
      logger.log(Level.WARNING, e, () -> "get file error, id " + id);
    }
    return null;
  }

  public String getBucketName() {
    return bucketName;
  }

  public OSS getOss() {
    return oss;
  }

  public String store(String id, Resource resource, String... userMetadata) {
    shouldNotBlank(id);
    shouldNotNull(resource);
    Map<String, Object> meta = defaultObject(resource.getMetadata(), Collections::emptyMap);
    Object contentType = meta.getOrDefault(Resource.META_CONTENT_TYPE,
        defaultObject(FileUtils.getContentType(resource.getName()), EMPTY));
    ObjectMetadata ossMeta = new ObjectMetadata();
    ossMeta.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
    ossMeta.setContentType(contentType.toString());
    ossMeta.setContentDisposition(new ContentDisposition(null, resource.getName(),
        resource.getName(), StandardCharsets.UTF_8, getMapLong(meta, Resource.META_CONTENT_LENGTH),
        null, getMapZonedDateTime(meta, Resource.META_LAST_MODIFIED), null).toString());
    Map<String, String> userMetadataMap = mapOf((Object[]) userMetadata);
    userMetadataMap.forEach(ossMeta::addUserMetadata);
    try (InputStream is = resource.openInputStream()) {
      oss.putObject(new PutObjectRequest(bucketName, id, is, ossMeta));
      return id;
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected void destroy() {
    oss.shutdown();
  }

  /**
   * corant-modules-cloud-alibaba
   *
   * @author bingo 下午12:17:51
   *
   */
  public static class OSSResource implements Resource {

    protected OSSObject object;
    protected String location;

    public OSSResource(OSSObject object, String location) {
      this.object = object;
      this.location = location;
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public Map<String, Object> getMetadata() {
      Map<String, Object> meta = new HashMap<>();
      meta.put("requestId", object.getRequestId());
      if (object.getObjectMetadata().getRawMetadata() != null) {
        meta.putAll(object.getObjectMetadata().getRawMetadata());
      }
      if (object.getObjectMetadata().getUserMetadata() != null) {
        meta.putAll(object.getObjectMetadata().getUserMetadata());
      }
      return meta;
    }

    @Override
    public String getName() {
      return object.getKey();
    }

    public OSSObject getOSSObject() {
      return object;
    }

    @Override
    public SourceType getSourceType() {
      return SourceType.UNKNOWN;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return object.getObjectContent();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
      if (OSSResource.class.isAssignableFrom(cls)) {
        return cls.cast(this);
      }
      return Resource.super.unwrap(cls);
    }
  }
}
