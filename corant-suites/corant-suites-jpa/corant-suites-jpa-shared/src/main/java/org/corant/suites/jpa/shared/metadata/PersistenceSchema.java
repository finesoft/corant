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
package org.corant.suites.jpa.shared.metadata;

import static org.corant.shared.util.StringUtils.replace;
import java.io.IOException;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.ClassPathResource;
import org.corant.shared.util.ValidateUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午11:48:32
 *
 */
public enum PersistenceSchema {

  V1_0("1.0"), V2_0("2.0"), V2_1("2.1"), V2_2("2.2");

  private final String version;

  private final transient Schema schema;

  private PersistenceSchema(String version) {
    this.version = version;
    schema = getSchema(version);
  }

  public static List<Exception> validate(Document document, String version) {
    for (PersistenceSchema ps : PersistenceSchema.values()) {
      if (ps.getVersion().equalsIgnoreCase(version)) {
        return ValidateUtils.validateXmlDocument(document, ps.getSchema());
      }
    }
    return null;
  }

  public Schema getSchema() {
    return schema;
  }

  public String getVersion() {
    return version;
  }

  private Schema getSchema(String version) {
    try {
      String xsdUrlPrefix = replace(getClass().getPackage().getName(), ".", "/") + "/persistence_";
      return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
          .newSchema(Resources.fromClassPath(xsdUrlPrefix + replace(version, ".", "_") + ".xsd")
              .map(ClassPathResource::getUrl).findFirst().get());
    } catch (SAXException | IOException e) {
      throw new CorantRuntimeException(e);
    }
  }
}
