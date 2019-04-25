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
package org.corant.suites.query.shared.mapping;

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.StringUtils.split;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.ClassPathResource;
import org.corant.shared.util.StringUtils;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:56:43
 *
 */
public class QueryParser {

  public static final String SCHEMA_URL = "org/corant/suites/query/shared/mapping/nqms_1_0.xsd";

  static Logger logger = Logger.getLogger(QueryParser.class.getName());

  public List<QueryMapping> parse(String pathExpress) {
    List<QueryMapping> qmList = new ArrayList<>();
    final QueryParserErrorHandler errHdl = new QueryParserErrorHandler();
    final SAXParserFactory factory = createSAXParserFactory();
    final Map<String, URL> fileMap = getQueryMappingFiles(pathExpress);
    for (Entry<String, URL> entry : fileMap.entrySet()) {
      logger.info(() -> String.format("Parse query mapping file %s.", entry.getKey()));
      try (InputStream is = entry.getValue().openStream()) {
        QueryParseHandler handler = new QueryParseHandler(entry.getKey());
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setErrorHandler(errHdl);
        reader.setContentHandler(handler);
        reader.parse(new InputSource(is));
        qmList.add(handler.getMapping());
      } catch (IOException | SAXException | ParserConfigurationException ex) {
        String errMsg = String.format("Parse query mapping file [%s] error!", entry.getKey());
        throw new QueryRuntimeException(ex, errMsg);
      }
    }
    return qmList;
  }

  SAXParserFactory createSAXParserFactory() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setSchema(getSchema());
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    return factory;
  }

  Map<String, URL> getQueryMappingFiles(String pathExpress) {
    Map<String, URL> map = new HashMap<>();
    asSet(split(pathExpress, ",")).stream().filter(StringUtils::isNotBlank).forEach(path -> {
      try {
        Resources.fromClassPath(path).forEach(f -> map.put(f.getResourceName(), f.getUrl()));
      } catch (Exception e) {
        throw new QueryRuntimeException(e);
      }
    });
    return map;
  }

  Schema getSchema() {
    try {
      return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
          Resources.fromClassPath(SCHEMA_URL).map(ClassPathResource::getUrl).findFirst().get());
    } catch (SAXException | IOException e) {
      throw new QueryRuntimeException(e);
    }
  }

  static class QueryParserErrorHandler implements ErrorHandler {
    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw new QueryRuntimeException(exception);
    }
  }
}
