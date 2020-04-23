package org.corant.suites.query.mongodb;

import org.corant.suites.query.shared.NamedQueryService;

import java.util.List;

/**
 * @auther sushuaihao 2020/4/22
 * @since
 */
public interface MgNamedQueryService extends NamedQueryService {

  <T> List<T> aggregate(String q, Object param);
}
