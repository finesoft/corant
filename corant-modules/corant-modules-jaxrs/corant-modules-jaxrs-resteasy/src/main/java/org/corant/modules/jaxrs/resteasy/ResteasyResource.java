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
package org.corant.modules.jaxrs.resteasy;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.corant.modules.servlet.ContentDispositions.parse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.corant.modules.jaxrs.shared.AbstractJaxrsResource;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.SourceType;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 上午11:02:48
 *
 */
public class ResteasyResource extends AbstractJaxrsResource {

  /**
   * Parse multipart/form-data
   *
   * @param uploadForm
   * @param fieldNames
   * @return
   * @throws IOException
   */
  protected Map<String, Object> parseFormFields(Map<String, List<InputPart>> uploadForm,
      String... fieldNames) throws IOException {
    if (isEmpty(uploadForm) || isEmpty(fieldNames)) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> map = new LinkedHashMap<>(uploadForm.size());
    for (String fieldName : fieldNames) {
      List<Object> lp = new ArrayList<>();
      if (uploadForm.get(fieldName) != null) {
        for (InputPart ip : uploadForm.get(fieldName)) {
          if (ip != null) {
            ContentDisposition cd = parse(ip.getHeaders().getFirst(CONTENT_DISPOSITION));
            if (cd.getFilename() != null) {
              lp.add(new InputPartResource(ip, cd));
            } else {
              lp.add(ip.getBodyAsString());
            }
          }
        }
      }
      if (lp.size() > 1) {
        map.put(fieldName, lp);
      } else if (lp.size() == 1) {
        map.put(fieldName, lp.get(0));
      } else {
        map.put(fieldName, null);
      }
    }
    return map;
  }

  /**
   * corant-modules-jaxrs-resteasy
   *
   * resteasy InputPart resource
   *
   * @author don
   * @date 2019-09-26
   *
   */
  public static class InputPartResource implements Resource {

    protected InputPart inputPart;

    protected String filename;

    protected Map<String, Object> metaData;

    public InputPartResource(InputPart inputPart) {
      this(shouldNotNull(inputPart), parse(inputPart.getHeaders().getFirst(CONTENT_DISPOSITION)));
    }

    InputPartResource(InputPart inputPart, ContentDisposition disposition) {
      this.inputPart = inputPart;
      String filename = disposition.getFilename();
      if (filename != null) {
        if (filename.startsWith("=?") && filename.endsWith("?=")) {
          // For RFC 2047 bingo 2021-04-06
          filename = DecoderUtil.decodeEncodedWords(filename, DecodeMonitor.SILENT);
        } else if (disposition.getCharset() == null && isNotBlank(filename)) {
          // 因为apache mime4j 解析浏览器提交的文件名按ISO_8859_1处理
          // 上传文件断点ContentUtil.decode(ByteSequence byteSequence, int offset, int length)
          filename = new String(filename.getBytes(ISO_8859_1), UTF_8);
        }
      }
      metaData = new HashMap<>();
      this.filename = defaultObject(filename, () -> "unnamed-" + UUID.randomUUID());
      metaData.put(META_NAME, this.filename);
      if (inputPart.getMediaType() != null) {
        metaData.put(META_CONTENT_TYPE, inputPart.getMediaType().toString());
      }
      if (disposition.getModificationDate() != null) {
        metaData.put(META_LAST_MODIFIED,
            disposition.getModificationDate().toInstant().toEpochMilli());
      }
      metaData.put(META_CONTENT_LENGTH, disposition.getSize());
    }

    public void addMetadata(String key, Object value) {
      metaData.put(key, value);
    }

    public String getContentType() {
      return inputPart.getMediaType().toString();
    }

    public InputPart getInputPart() {
      return inputPart;
    }

    @Override
    public String getLocation() {
      return getName();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return unmodifiableMap(metaData);
    }

    @Override
    public String getName() {
      return filename;
    }

    @Override
    public SourceType getSourceType() {
      return null;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return inputPart.getBody(InputStream.class, null);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
      if (InputPartResource.class.isAssignableFrom(cls)) {
        return cls.cast(this);
      }
      return Resource.super.unwrap(cls);
    }

  }
}
