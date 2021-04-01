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

import static org.corant.shared.util.Empties.isEmpty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.corant.modules.jaxrs.shared.AbstractJaxrsResource;
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
      List<String> lp = new ArrayList<>();
      if (uploadForm.get(fieldName) != null) {
        for (InputPart ip : uploadForm.get(fieldName)) {
          if (ip != null) {
            lp.add(ip.getBodyAsString());
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
}
