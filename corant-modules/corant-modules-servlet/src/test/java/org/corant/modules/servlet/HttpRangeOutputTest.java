package org.corant.modules.servlet;
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

import static org.corant.shared.util.Functions.uncheckedFunction;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.corant.modules.servlet.HttpRanges.HttpRange;
import org.corant.shared.resource.LimitedStream.RangedInputStream;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Streams;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 上午11:13:03
 *
 */
public class HttpRangeOutputTest extends TestCase {

  @Test
  public void testRangeSplit() throws IOException {
    long lastPos = 0;
    File file = new File("D:\\bingo_new.xlsx");
    List<HttpRange> useRanges =
        HttpRanges.parseRanges("bytes=0-200,201-300,301-400,401-3000,3001-", file.length());
    FileInputStream is = new FileInputStream(file);
    int i = 0;
    List<String> paths = new ArrayList<>();
    for (HttpRange range : useRanges) {
      final String path = "D:\\bingo_new___" + i++;
      try (FileOutputStream os = new FileOutputStream(path)) {
        Streams.copy(new RangedInputStream(is, range.start() - lastPos, range.size()), os);
      }
      lastPos = range.end() + 1;
      paths.add(path);
    }
    final String md = "D:\\bingo_new___merged";
    try (
        InputStream isx = Streams.concat(paths.stream()
            .map(uncheckedFunction(FileInputStream::new)).collect(Collectors.toList()));
        FileOutputStream fos = new FileOutputStream(md)) {
      Streams.copy(isx, fos);
    }
    assertTrue(FileUtils.isSameContent(file, new File(md)));
    paths.forEach(p -> new File(p).delete());
    new File(md).delete();
  }

}
