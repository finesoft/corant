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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import org.corant.shared.util.ConversionUtils;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.suites.query.shared.mapping.Properties.Property;
import org.corant.suites.query.shared.mapping.QueryHint.QueryHintParameter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:24:51
 *
 */
public class QueryParseHandler extends DefaultHandler {

  private final String url;
  private final List<Query> queries = new ArrayList<>();
  private final List<ParameterMapping> paraMappings = new ArrayList<>();
  private String commonSegment;
  private QueryMapping mapping;

  private final Stack<Object> valueStack = new Stack<>();
  private final Stack<String> nameStack = new Stack<>();
  private final StringBuilder charStack = new StringBuilder();

  public QueryParseHandler(String url) {
    this.url = url;
    mapping = new QueryMapping();
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String cqn = currentQName();
    if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(cqn) || SchemaNames.X_DESC.equalsIgnoreCase(cqn)
        || SchemaNames.X_SCRIPT.equalsIgnoreCase(cqn)) {
      charStack.append(ch, start, length);
    }
  }

  @Override
  public void endDocument() throws SAXException {
    mapping.setCommonSegment(commonSegment);
    mapping.paraMapping
        .putAll(paraMappings.stream().collect(Collectors.toMap(p -> p.getName(), p -> p)));
    mapping.queries.addAll(queries);
    valueStack.clear();
    nameStack.clear();
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (SchemaNames.X_ENTRY.equalsIgnoreCase(qName)) {
      handleParamEntry(false, qName, null);
    } else if (SchemaNames.QUE_ELE.equalsIgnoreCase(qName)) {
      handleQuery(false, qName, null);
    } else if (SchemaNames.QUE_FQE_ELE.equalsIgnoreCase(qName)) {
      handleFetchQuery(false, qName, null);
    } else if (SchemaNames.X_PARAM.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof FetchQueryParameter) {
        handleFetchQueryParameter(false, qName, null);
      } else if (currentObject() instanceof QueryHintParameter) {
        handleQueryHintParameter(false, qName, null);
      }
    } else if (SchemaNames.QUE_HINT_ELE.equalsIgnoreCase(qName)) {
      handleQueryHint(false, qName, null);
    } else if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(qName)) {
      handleCommonSegment(false, qName, null);
    } else if (SchemaNames.X_DESC.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryDesc(false, qName, null);
      }
    } else if (SchemaNames.X_PROS.equalsIgnoreCase(qName)) {
      handleQueryProperties(false, qName, null);
    } else if (SchemaNames.X_PRO.equalsIgnoreCase(qName)) {
      handleProperty(false, qName, null);
    } else if (SchemaNames.X_SCRIPT.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryScript(false, qName, null);
      } else if (currentObject() instanceof QueryHint) {
        handleQueryHintScript(false, qName, null);
      } else if (currentObject() instanceof FetchQuery) {
        handleFetchQueryScript(false, qName, null);
      }
    }
  }

  public QueryMapping getMapping() {
    return mapping;
  }

  @Override
  public void startDocument() throws SAXException {
    mapping.url = url;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (SchemaNames.X_ENTRY.equalsIgnoreCase(qName)) {
      handleParamEntry(true, qName, attributes);
    } else if (SchemaNames.QUE_ELE.equalsIgnoreCase(qName)) {
      handleQuery(true, qName, attributes);
    } else if (SchemaNames.QUE_FQE_ELE.equalsIgnoreCase(qName)) {
      handleFetchQuery(true, qName, attributes);
    } else if (SchemaNames.X_PARAM.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof FetchQuery) {
        handleFetchQueryParameter(true, qName, attributes);
      } else if (currentObject() instanceof QueryHint) {
        handleQueryHintParameter(true, qName, attributes);
      }
    } else if (SchemaNames.QUE_HINT_ELE.equalsIgnoreCase(qName)) {
      handleQueryHint(true, qName, attributes);
    } else if (SchemaNames.COMMON_SGEMENT.equalsIgnoreCase(qName)) {
      handleCommonSegment(true, qName, attributes);
    } else if (SchemaNames.X_DESC.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryDesc(true, qName, attributes);
      }
    } else if (SchemaNames.X_PROS.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryProperties(true, qName, attributes);
      }
    } else if (SchemaNames.X_PRO.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Properties) {
        handleProperty(true, qName, attributes);
      }
    } else if (SchemaNames.X_SCRIPT.equalsIgnoreCase(qName)) {
      if (currentObject() instanceof Query) {
        handleQueryScript(true, qName, attributes);
      } else if (currentObject() instanceof QueryHint) {
        handleQueryHintScript(true, qName, attributes);
      } else if (currentObject() instanceof FetchQuery) {
        handleFetchQueryScript(true, qName, attributes);
      }
    }
  }

  void handleCommonSegment(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String segment = charStack.toString();
      charStack.delete(0, charStack.length());
      commonSegment = segment.trim();
      nameStack.pop();
    }
  }

  void handleFetchQuery(boolean start, String qName, Attributes attributes) {
    if (start) {
      FetchQuery fq = new FetchQuery();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i), atv = attributes.getValue(i);
        if (SchemaNames.FQE_ATT_NAME.equalsIgnoreCase(aqn)) {
          fq.setReferenceQuery(atv);
        } else if (SchemaNames.FQE_ATT_MAX_SIZE.equalsIgnoreCase(aqn)) {
          fq.setMaxSize(ConversionUtils.toInteger(atv));
        } else if (SchemaNames.FQE_ATT_PRO_NAME.equalsIgnoreCase(aqn)) {
          fq.setInjectPropertyName(atv);
        } else if (SchemaNames.FQE_ATT_VER.equalsIgnoreCase(aqn)) {
          fq.setReferenceQueryversion(defaultString(atv));
        } else if (SchemaNames.QUE_ATT_RST_CLS.equalsIgnoreCase(aqn)) {
          fq.setResultClass(isBlank(atv) ? java.util.Map.class : tryAsClass(atv));
        } else if (SchemaNames.FQE_ATT_MULT.equalsIgnoreCase(aqn)) {
          fq.setMultiRecords(isBlank(atv) ? true : ConversionUtils.toBoolean(atv));
        }
      }
      valueStack.push(fq);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse error the fetch query must be in query element!");
      }
      q.getFetchQueries().add((FetchQuery) obj);
      nameStack.pop();
    }
  }

  void handleFetchQueryParameter(boolean start, String qName, Attributes attributes) {
    if (start) {
      FetchQueryParameter fqp = new FetchQueryParameter();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i), atv = attributes.getValue(i);
        if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
          fqp.setName(atv);
        } else if (SchemaNames.FQE_ELE_PARAM_ATT_SRC.equalsIgnoreCase(aqn)) {
          fqp.setSource(ConversionUtils.toEnum(atv, FetchQueryParameterSource.class));
        } else if (SchemaNames.FQE_ELE_PARAM_ATT_SRC_NME.equalsIgnoreCase(aqn)) {
          fqp.setSourceName(atv);
        } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
          fqp.setValue(atv);
        }
      }
      valueStack.push(fqp);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      FetchQuery q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException(
            "Parse error the fetch query parameter must be in fetch query element!");
      }
      q.getParameters().add((FetchQueryParameter) obj);
      nameStack.pop();
    }
  }

  void handleFetchQueryScript(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String script = charStack.toString();
      charStack.delete(0, charStack.length());
      FetchQuery q = this.currentObject();
      if (q == null || isBlank(script)) {
        throw new QueryRuntimeException(
            "Parse error the query script must be in query element and script can't null!");
      }
      q.setScript(script.trim());
      nameStack.pop();
    }
  }

  void handleParamEntry(boolean start, String qName, Attributes attributes) {
    if (start) {
      ParameterMapping pm = new ParameterMapping();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i), atv = attributes.getValue(i);
        if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
          pm.setName(atv);
        } else if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn)) {
          pm.setType(tryAsClass(atv));
        }
      }
      valueStack.push(pm);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      paraMappings.add((ParameterMapping) obj);
      nameStack.pop();
    }
  }

  void handleProperty(boolean start, String qName, Attributes attributes) {
    if (start) {
      Property pm = new Property();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i), atv = attributes.getValue(i);
        if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
          pm.setName(atv);
        } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
          pm.setValue(atv);
        }
      }
      valueStack.push(pm);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Properties ps = this.currentObject();
      ps.add((Property) obj);
      nameStack.pop();
    }
  }

  void handleQuery(boolean start, String qName, Attributes attributes) {
    if (start) {
      Query q = new Query();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i), atv = attributes.getValue(i);
        if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
          q.setName(atv);
        } else if (SchemaNames.QUE_ATT_CACHE.equalsIgnoreCase(aqn)) {
          q.setCache(ConversionUtils.toBoolean(atv));
        } else if (SchemaNames.QUE_ATT_CACHE_RS_MD.equalsIgnoreCase(aqn)) {
          q.setCacheResultSetMetadata(ConversionUtils.toBoolean(atv));
        } else if (SchemaNames.QUE_ATT_RST_CLS.equalsIgnoreCase(aqn)) {
          q.setResultClass(isBlank(atv) ? java.util.Map.class : tryAsClass(atv));
        } else if (SchemaNames.QUE_ATT_RST_SET_CLS.equalsIgnoreCase(aqn)) {
          q.setResultSetMapping(isBlank(atv) ? null : tryAsClass(atv));
        } else if (SchemaNames.QUE_ATT_VER.equalsIgnoreCase(aqn)) {
          q.setVersion(defaultString(atv));
        }
      }
      valueStack.push(q);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      queries.add((Query) obj);
      nameStack.pop();
    }
  }

  void handleQueryDesc(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String desc = charStack.toString();
      charStack.delete(0, charStack.length());
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException(
            "Parse error the query description must be in query element!");
      }
      q.setDescription(desc.trim());
      nameStack.pop();
    }
  }

  void handleQueryHint(boolean start, String qName, Attributes attributes) {
    if (start) {
      QueryHint hit = new QueryHint();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i);
        String atv = attributes.getValue(i);
        if (SchemaNames.X_KEY.equalsIgnoreCase(aqn)) {
          hit.setKey(atv);
        }
      }
      valueStack.push(hit);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse error the query hit must be in query element!");
      }
      q.getHints().add((QueryHint) obj);
      nameStack.pop();
    }
  }

  void handleQueryHintParameter(boolean start, String qName, Attributes attributes) {
    if (start) {
      QueryHintParameter qhp = new QueryHintParameter();
      for (int i = 0; i < attributes.getLength(); i++) {
        String aqn = attributes.getQName(i);
        String atv = attributes.getValue(i);
        if (SchemaNames.X_NAME.equalsIgnoreCase(aqn)) {
          qhp.setName(atv);
        } else if (SchemaNames.X_VALUE.equalsIgnoreCase(aqn)) {
          qhp.setValue(atv);
        } else if (SchemaNames.X_TYPE.equalsIgnoreCase(aqn)) {
          qhp.setType(atv);
        }
      }
      valueStack.push(qhp);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      QueryHint qh = this.currentObject();
      if (qh == null) {
        throw new QueryRuntimeException(
            "Parse error the query hint parameter must be in query hint element!");
      }
      if (obj instanceof QueryHintParameter) {
        qh.addParameter(QueryHintParameter.class.cast(obj));
      }
      nameStack.pop();
    }
  }

  void handleQueryHintScript(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String script = charStack.toString();
      charStack.delete(0, charStack.length());
      QueryHint q = this.currentObject();
      if (q == null || isBlank(script)) {
        throw new QueryRuntimeException(
            "Parse error the query hit script must be in query element and script can't null!");
      }
      q.setScript(script.trim());
      nameStack.pop();
    }
  }

  void handleQueryProperties(boolean start, String qName, Attributes attributes) {
    if (start) {
      Properties fq = new Properties();
      valueStack.push(fq);
      nameStack.push(qName);
    } else {
      Object obj = valueStack.pop();
      Query q = this.currentObject();
      if (q == null) {
        throw new QueryRuntimeException("Parse error the fetch query must be in query element!");
      }
      ((Properties) obj).toMap().forEach((k, v) -> q.addProperty(k, v));
      nameStack.pop();
    }
  }

  void handleQueryScript(boolean start, String qName, Attributes attributes) {
    if (start) {
      nameStack.push(qName);
    } else {
      String script = charStack.toString();
      charStack.delete(0, charStack.length());
      Query q = this.currentObject();
      if (q == null || isBlank(script)) {
        throw new QueryRuntimeException(
            "Parse error the query script must be in query element and script can't null!");
      }
      q.setScript(script.trim());
      nameStack.pop();
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T currentObject() {
    return valueStack.isEmpty() ? null : (T) valueStack.peek();
  }

  private String currentQName() {
    return nameStack.isEmpty() ? null : nameStack.peek();
  }
}
