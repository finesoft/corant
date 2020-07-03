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
package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

/**
 * corant-suites-elastic-ext
 *
 * @author bingo 下午11:18:51
 *
 */
public class MixedZhEnNGramTokenFilterFactory extends AbstractTokenFilterFactory {

  private final int zhMinGram;
  private final int zhMaxGram;
  private final int enMinGram;
  private final int enMaxGram;
  private final boolean retainToken;

  /**
   *
   * @param indexSettings
   * @param env
   * @param name
   * @param settings
   */
  public MixedZhEnNGramTokenFilterFactory(IndexSettings indexSettings, Environment env, String name,
      Settings settings) {
    super(indexSettings, name, settings);
    zhMinGram =
        settings.getAsInt("zh_min_gram", MixedZhEnNGramTokenFilter.ZH_DEFAULT_MIN_NGRAM_SIZE);
    zhMaxGram =
        settings.getAsInt("zh_max_gram", MixedZhEnNGramTokenFilter.ZH_DEFAULT_MAX_NGRAM_SIZE);
    enMinGram =
        settings.getAsInt("en_min_gram", MixedZhEnNGramTokenFilter.EN_DEFAULT_MIN_NGRAM_SIZE);
    enMaxGram =
        settings.getAsInt("en_max_gram", MixedZhEnNGramTokenFilter.EN_DEFAULT_MAX_NGRAM_SIZE);
    retainToken = settings.getAsBoolean("retain_token", MixedZhEnNGramTokenFilter.RETAIN_TOKEN);
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    return new MixedZhEnNGramTokenFilter(tokenStream, zhMinGram, zhMaxGram, enMinGram, enMaxGram,
        retainToken);
  }

}
