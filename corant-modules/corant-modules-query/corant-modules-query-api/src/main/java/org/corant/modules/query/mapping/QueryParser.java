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
package org.corant.modules.query.mapping;

import static java.lang.String.format;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.split;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Resources;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * corant-modules-query-api
 *
 * @author bingo 上午10:56:43
 */
public class QueryParser {

  public static final String SCHEMA_URL = "org/corant/modules/query/mapping/nqms_1_0.xsd";

  static Logger logger = Logger.getLogger(QueryParser.class.getName());

  public List<QueryMapping> parse(String... pathExpresses) {
    List<QueryMapping> qmList = new CopyOnWriteArrayList<>();
    final SAXParserFactory factory = createSAXParserFactory();
    final Map<String, Resource> fileMap = getQueryMappingFiles(pathExpresses);
    fileMap.entrySet().stream().parallel().forEach(entry -> {
      logger.fine(() -> format("Parse query mapping file %s.", entry.getKey()));
      try (InputStream is = entry.getValue().openInputStream()) {
        QueryParseHandler handler = new QueryParseHandler(entry.getKey());
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setErrorHandler(new QueryParserErrorHandler(entry.getValue().getLocation()));
        reader.setContentHandler(handler);
        reader.parse(new InputSource(is));
        qmList.add(handler.getMapping());
      } catch (IOException | SAXException | ParserConfigurationException ex) {
        throw new QueryRuntimeException(ex, "Parse query mapping file [%s] error!", entry.getKey());
      }
    });
    return qmList;
  }

  SAXParserFactory createSAXParserFactory() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setSchema(getSchema());
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    return factory;
  }

  Map<String, Resource> getQueryMappingFiles(String... pathExpresses) {
    Map<String, Resource> map = new ConcurrentHashMap<>();
    for (String pathExpress : pathExpresses) {
      setOf(split(pathExpress, ",", true, true)).forEach(path -> {
        try {
          Resources.from(path).forEach(f -> map.put(f.getURL().getPath(), f));
        } catch (Exception e) {
          throw new QueryRuntimeException(e, "Can't resolve query mapping files from path %s.",
              path);
        }
      });
    }
    return map;
  }

  Schema getSchema() {
    try {
      return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
          Resources.fromClassPath(SCHEMA_URL).map(ClassPathResource::getURL).findFirst().get());
    } catch (SAXException | IOException e) {
      throw new QueryRuntimeException(e, "Can't resolve query mapping XML schema from %s.",
          SCHEMA_URL);
    }
  }

  static class QueryParserErrorHandler implements ErrorHandler {

    final String url;

    QueryParserErrorHandler(String url) {
      this.url = url;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception, "Parse %s error!", url);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception, "Parse %s error!", url);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception, "Parse %s error!", url);
    }
  }
}
