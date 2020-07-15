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
package org.corant.suites.query.elastic;

import static org.corant.shared.util.Sets.setOf;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.join.query.ParentIdQueryBuilder;
import org.elasticsearch.search.SearchModule;

/**
 * corant-suites-query
 *
 * @author bingo 下午10:19:30
 *
 */
public class XContentUtils {

  static final SearchModule SM = new SearchModule(Settings.EMPTY, false, Collections.emptyList());

  // For some unknown reason, es did not register the following content to SearchModule, we do it...
  static {
    // support parent_id
    SM.getNamedWriteables().add(new NamedWriteableRegistry.Entry(QueryBuilder.class,
        ParentIdQueryBuilder.NAME, ParentIdQueryBuilder::new));
    SM.getNamedXContents().add(new NamedXContentRegistry.Entry(QueryBuilder.class,
        new ParseField(ParentIdQueryBuilder.NAME), (p, c) -> ParentIdQueryBuilder.fromXContent(p)));
    // support has child
    SM.getNamedWriteables().add(new NamedWriteableRegistry.Entry(QueryBuilder.class,
        HasChildQueryBuilder.NAME, HasChildQueryBuilder::new));
    SM.getNamedXContents().add(new NamedXContentRegistry.Entry(QueryBuilder.class,
        new ParseField(HasChildQueryBuilder.NAME), (p, c) -> HasChildQueryBuilder.fromXContent(p)));
    // support has parent
    SM.getNamedWriteables().add(new NamedWriteableRegistry.Entry(QueryBuilder.class,
        HasParentQueryBuilder.NAME, HasParentQueryBuilder::new));
    SM.getNamedXContents()
        .add(new NamedXContentRegistry.Entry(QueryBuilder.class,
            new ParseField(HasParentQueryBuilder.NAME),
            (p, c) -> HasParentQueryBuilder.fromXContent(p)));
  }
  static final NamedXContentRegistry NCR =
      new NamedXContentRegistry(new ArrayList<>(SM.getNamedXContents()));
  static final NamedWriteableRegistry NWR =
      new NamedWriteableRegistry(new ArrayList<>(SM.getNamedWriteables()));

  public static XContentParser createParser(XContent xContent, byte[] data) throws IOException {
    return xContent.createParser(xContentRegistry(), DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
        data);
  }

  public static XContentParser createParser(XContent xContent, BytesReference data)
      throws IOException {
    return xContent.createParser(xContentRegistry(), DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
        data.streamInput());
  }

  public static XContentParser createParser(XContent xContent, InputStream data)
      throws IOException {
    return xContent.createParser(xContentRegistry(), DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
        data);
  }

  public static XContentParser createParser(XContent xContent, String data) throws IOException {
    return xContent.createParser(xContentRegistry(), DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
        data);
  }

  public static XContentParser createParser(XContentBuilder builder) throws IOException {
    return builder.generator().contentType().xContent().createParser(xContentRegistry(),
        DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
        BytesReference.bytes(builder).streamInput());
  }

  public static Map<String, Object> searchResponseToMap(SearchResponse searchResponse,
      String... paths) throws IOException {
    if (searchResponse == null) {
      return new HashMap<>();
    } else {
      return XContentHelper.convertToMap(
          BytesReference
              .bytes(searchResponse.toXContent(new XContentBuilder(XContentType.JSON.xContent(),
                  new ByteArrayOutputStream(), setOf(paths)), ToXContent.EMPTY_PARAMS)),
          false, XContentType.JSON).v2();
    }
  }

  public static NamedWriteableRegistry writableRegistry() {
    return NWR;
  }

  public static NamedXContentRegistry xContentRegistry() {
    return NCR;
  }

}
