package org.corant.modules.query.mongodb;

import java.util.List;
import org.corant.modules.query.shared.NamedQueryService;

/**
 * corant-modules-query-mongodb
 *
 * @author sushuaihao 2020/4/22
 * @since
 */
public interface MgNamedQueryService extends NamedQueryService {

  <T> List<T> aggregate(String q, Object param);
}
