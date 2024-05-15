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

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ContentDispositions.parse;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
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
import java.util.stream.Collectors;
import jakarta.ws.rs.core.GenericType;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.corant.modules.jaxrs.shared.AbstractJaxrsResource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.SourceType;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.ContentDispositions.ContentDisposition;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
 * corant-modules-jaxrs-resteasy
 * @author bingo 上午11:02:48
 */
public class ResteasyResource extends AbstractJaxrsResource {

  /**
   * Parse multipart/form-data
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
   * @author bingo 上午12:42:18
   */
  public static class MultipartFormDataExtractor implements AutoCloseable {

    final MultipartFormDataInput formData;
    final Map<String, List<InputPart>> formDataMap;

    public MultipartFormDataExtractor(MultipartFormDataInput formData) {
      this.formData = shouldNotNull(formData);
      formDataMap = formData.getFormDataMap();
    }

    @Override
    public void close() throws Exception {
      formData.close();
    }

    /**
     * Returns and converts the single filed value by given class.
     * @param <T>       the expected field type
     * @param fieldName the field name to get
     * @param clazz     the expected field class
     * @return the field value or null, if the field is not found or the field value is found to be
     * null
     */
    public <T> T getField(String fieldName, Class<T> clazz) {
      List<InputPart> ips = formDataMap.get(fieldName);
      InputPart part;
      if (isNotEmpty(ips) && (part = ips.get(0)) != null) {
        try {
          return part.getBody(clazz, null);
        } catch (IOException ex) {
          throw new CorantRuntimeException(ex);
        }
      }
      return null;
    }

    /**
     * Returns and converts the single filed value by given class.
     * @param <T>       the expected field type
     * @param fieldName the field name to get
     * @param type      the expected field generic type
     * @return the field value or null, if the field is not found or the field value is found to be
     * null
     * @see GenericType
     */
    public <T> T getField(String fieldName, GenericType<T> type) {
      List<InputPart> ips = formDataMap.get(fieldName);
      InputPart part;
      if (isNotEmpty(ips) && (part = ips.get(0)) != null) {
        try {
          return part.getBody(type);
        } catch (IOException ex) {
          throw new CorantRuntimeException(ex);
        }
      }
      return null;
    }

    /**
     * Returns a list of values for a set of fields with the same given name, or empty list if the
     * field not found. Note: the returns field value list is unmodifiable.
     * @param <T>
     * @param fieldName
     * @param clazz
     * @return getFields
     */
    public <T> List<T> getFields(String fieldName, Class<T> clazz) {
      List<InputPart> ips = formDataMap.get(fieldName);
      if (isNotEmpty(ips)) {
        List<T> list = new ArrayList<>();
        for (InputPart ip : ips) {
          if (ip != null) {
            try {
              list.add(ip.getBody(clazz, null));
            } catch (IOException e) {
              throw new CorantRuntimeException(e);
            }
          }
        }
        return unmodifiableList(list);
      }
      return emptyList();
    }

    /**
     * Returns a matrix of all field value lists based on the given field name array, where each
     * element (Map) in the list corresponds to the values of multiple fields, used to process
     * tabular data forms.
     * @param fieldNames the field names
     * @return the value lists
     */
    public List<Map<String, Object>> getFieldsMatrix(String... fieldNames) {
      if (fieldNames.length > 0) {
        Map<String, List<Object>> temp = new LinkedHashMap<>(fieldNames.length);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String fieldName : fieldNames) {
          List<InputPart> ips = formDataMap.get(fieldName);
          int valueSize = sizeOf(ips);
          int resultSize = result.size();
          if (valueSize > 0) {
            for (int i = 0; i < valueSize - resultSize; i++) {
              result.add(new LinkedHashMap<>());
            }
            List<Object> fieldValues = new ArrayList<>(valueSize);
            for (InputPart ip : ips) {
              try {
                ContentDisposition cd = parse(ip.getHeaders().getFirst(CONTENT_DISPOSITION));
                if (cd.getFilename() != null) {
                  fieldValues.add(new InputPartResource(ip, cd));
                } else {
                  fieldValues.add(ip.getBody(String.class, null));
                }
              } catch (IOException e) {
                throw new CorantRuntimeException(e);
              }
            }
            temp.put(fieldName, fieldValues);
          }
        }
        temp.forEach((fn, fvs) -> {
          int fs = fvs.size();
          for (int i = 0; i < fs; i++) {
            result.get(i).put(fn, fvs.get(i));
          }
        });
        return unmodifiableList(result);
      }
      return emptyList();
    }

    /**
     * Returns an uploaded file resource based on the given field name, or throws an exception if
     * the specified field is not a file field.
     * @param fieldName the field name
     * @return the uploaded file.
     */
    public InputPartResource getFile(String fieldName) {
      List<InputPart> ips = formDataMap.get(fieldName);
      if (isNotEmpty(ips)) {
        return ips.stream()
            .map(ip -> Pair.of(parse(ip.getHeaders().getFirst(CONTENT_DISPOSITION)), ip))
            .filter(p -> p.left().getFilename() != null)
            .map(p -> new InputPartResource(p.right(), p.left())).findFirst().orElseThrow(
                () -> new CorantRuntimeException("The field name %s is not a file", fieldName));
      }
      return null;
    }

    /**
     * Returns a list of uploaded file resources with the same name according to the given field
     * name. If the specified field is not a file or the field value is empty, an empty list is
     * returned. Note: the returned list is unmodifiable.
     * @param fieldName
     * @return getFiles
     */
    public List<InputPartResource> getFiles(String fieldName) {
      List<InputPart> ips = formDataMap.get(fieldName);
      if (isNotEmpty(ips)) {
        return ips.stream()
            .map(ip -> Pair.of(parse(ip.getHeaders().getFirst(CONTENT_DISPOSITION)), ip))
            .filter(p -> p.left().getFilename() != null)
            .map(p -> new InputPartResource(p.right(), p.left()))
            .collect(Collectors.toUnmodifiableList());
      }
      return emptyList();
    }

  }
}
