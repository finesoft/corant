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

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.CodepointCountFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

/**
 *
 * corant-suites-elastic-ext
 *
 * Adjust NGram algorithm and add parameters of mixed Chinese and English slices
 *
 * @author bingo 下午11:17:24
 *
 */
public class MixedZhEnNGramTokenFilter extends TokenFilter {

  public static final int EN_DEFAULT_MAX_NGRAM_SIZE = 15;
  public static final int EN_DEFAULT_MIN_NGRAM_SIZE = 3;

  public static final int ZH_DEFAULT_MAX_NGRAM_SIZE = 2;
  public static final int ZH_DEFAULT_MIN_NGRAM_SIZE = 1;
  public static final boolean RETAIN_TOKEN = true;

  private char[] actualTermBuffer;
  private boolean retainToken = false;
  private boolean filledCurToken = false;

  private int curCodePointCount;
  private int curGramSize;
  private int curPos;
  private int curPosInc;
  private int curPosLen;
  private char[] curTermBuffer;
  private int curTermLength;
  private int minGram;
  private int maxGram;
  private int tokEnd;
  private int tokStart;
  private final int zhMinGram;
  private final int zhMaxGram;
  private final int enMinGram;
  private final int enMaxGram;

  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PositionIncrementAttribute posIncAtt;
  private final PositionLengthAttribute posLenAtt;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * 构造
   *
   * @param input 词流
   * @param zhMinGram 中文最小长度
   * @param zhMaxGram 中文最大长度
   * @param enMinGram 英文最小长度
   * @param enMaxGram 英文最大长度
   * @param retainToken 保留分词
   */
  protected MixedZhEnNGramTokenFilter(TokenStream input, int zhMinGram, int zhMaxGram,
      int enMinGram, int enMaxGram, boolean retainToken) {
    super(new CodepointCountFilter(input, 1, Integer.MAX_VALUE));
    if (zhMinGram < 1 || enMinGram < 1) {
      throw new IllegalArgumentException("minGram must be greater than zero");
    }
    if (zhMinGram > zhMaxGram || enMinGram > enMaxGram) {
      throw new IllegalArgumentException("minGram must not be greater than maxGram");
    }
    this.retainToken = retainToken;
    this.zhMinGram = zhMinGram;
    this.zhMaxGram = zhMaxGram;
    this.enMinGram = enMinGram;
    this.enMaxGram = enMaxGram;
    minGram = zhMinGram;
    maxGram = zhMaxGram;
    posIncAtt = addAttribute(PositionIncrementAttribute.class);
    posLenAtt = addAttribute(PositionLengthAttribute.class);
  }

  @Override
  public boolean incrementToken() throws IOException {
    while (true) {
      if (curTermBuffer == null) {
        if (!input.incrementToken()) {
          return false;
        } else {
          curTermBuffer = termAtt.buffer().clone();
          curTermLength = termAtt.length();
          curCodePointCount = Character.codePointCount(termAtt, 0, curTermLength);
          curPos = 0;
          curPosInc = posIncAtt.getPositionIncrement();
          curPosLen = posLenAtt.getPositionLength();
          tokStart = offsetAtt.startOffset();
          tokEnd = offsetAtt.endOffset();
          actualTermBuffer = Arrays.copyOf(curTermBuffer, curTermLength);
          int actualLen = actualTermBuffer.length;
          if (isZhTerm(actualTermBuffer)) {
            minGram = zhMinGram > actualLen ? actualLen : zhMinGram;
            maxGram = zhMaxGram;
          } else {
            minGram = enMinGram > actualLen ? actualLen : enMinGram;
            maxGram = enMaxGram;
          }
          curGramSize = minGram;
        }
      }

      if (curGramSize > maxGram || curPos + curGramSize > curCodePointCount) {
        ++curPos;
        curGramSize = minGram;
      }

      if (curPos + curGramSize <= curCodePointCount) {
        clearAttributes();
        final int start = Character.offsetByCodePoints(curTermBuffer, 0, curTermLength, 0, curPos);
        final int end =
            Character.offsetByCodePoints(curTermBuffer, 0, curTermLength, start, curGramSize);
        if (start == 1 && !filledCurToken && actualTermBuffer.length > maxGram
            && actualTermBuffer != null && retainToken) {
          termAtt.copyBuffer(actualTermBuffer, 0, actualTermBuffer.length);
          posIncAtt.setPositionIncrement(curPosInc);
          posLenAtt.setPositionLength(curPosLen);
          offsetAtt.setOffset(tokStart, tokEnd);
          filledCurToken = true;
          // watch(0, curTermLength);
          return true;
        }
        termAtt.copyBuffer(curTermBuffer, start, end - start);
        posIncAtt.setPositionIncrement(curPosInc);
        curPosInc = 0;
        posLenAtt.setPositionLength(curPosLen);
        offsetAtt.setOffset(tokStart, tokEnd);
        // watch(start, end);
        curGramSize++;
        return true;
      } else {
        filledCurToken = false;
      }
      curTermBuffer = null;
    }
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    curTermBuffer = null;
    actualTermBuffer = null;
  }

  void watch(int s, int e) {
    System.out.println("CTB: " + new String(curTermBuffer) + "\tCTL: " + curTermLength + "\tACTB: "
        + new String(actualTermBuffer) + "\t G: " + minGram + " ~ " + maxGram + "\tCPC: "
        + curCodePointCount + "\tT:" + new String(Arrays.copyOfRange(curTermBuffer, s, e))
        + "\t CG: " + curGramSize + "\tS-E: " + s + " ~ " + e + "\t RT: " + retainToken);
  }

  /**
   * 是否是中日韩字符
   *
   * @param c
   * @return
   */
  private boolean isZhChar(char c) {
    Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
    return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
        || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
        || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
        || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
  }

  /**
   * 判断是否是中文词，只要含有中文即可
   *
   * @param curTermBuffer
   * @return
   */
  private boolean isZhTerm(char[] curTermBuffer) {
    if (curTermBuffer == null || curTermBuffer.length == 0) {
      return false;
    }
    boolean zh = false;
    for (char c : curTermBuffer) {
      if (zh = isZhChar(c)) {
        break;
      }
    }
    return zh;
  }
}
