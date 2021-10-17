/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.resource;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.linkedListOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Functions;
import org.corant.shared.util.PathMatcher;
import org.corant.shared.util.PathMatcher.GlobMatcher;
import org.corant.shared.util.PathMatcher.RegexMatcher;

/**
 * corant-shared
 *
 * @author bingo 下午4:24:14
 *
 */
public class FileSystemResourceLoader implements ResourceLoader {

  public static final char UNIX_SEPARATOR = '/';
  public static final String UNIX_SEPARATOR_STR = Character.toString(UNIX_SEPARATOR);
  public static final char WINDOWS_SEPARATOR = '\\';
  public static final FileSystemResourceLoader DFLT_INST = new FileSystemResourceLoader();

  protected final boolean ignoreCase;

  public FileSystemResourceLoader() {
    this(true);
  }

  public FileSystemResourceLoader(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  /**
   * Search for files by file path or path expression, only return files without directories;
   * case-insensitive, support Glob and Regex file name expression search, if it is not a path
   * expression, return all files under the specified file or directory.
   *
   * @param location
   *
   * @see PathMatcher#decidePathMatcher(String, boolean, boolean)
   * @see GlobMatcher
   * @see RegexMatcher
   */
  @Override
  public List<FileSystemResource> load(Object location) throws IOException {
    String path = shouldNotNull(location).toString();
    String pathExp = SourceType.FILE_SYSTEM.resolve(path);
    pathExp = isNotBlank(pathExp) ? pathExp.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR) : pathExp;
    Optional<PathMatcher> matcher = PathMatcher.decidePathMatcher(pathExp, false, ignoreCase);
    if (matcher.isPresent()) {
      final PathMatcher useMatcher = matcher.get();
      return selectFiles(useMatcher.getPlainParent(UNIX_SEPARATOR_STR), f -> {
        try {
          return useMatcher.test(f.getCanonicalPath().replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      }).stream().map(FileSystemResource::new).collect(Collectors.toList());
    } else {
      return selectFiles(pathExp, null).stream().map(FileSystemResource::new)
          .collect(Collectors.toList());
    }
  }

  /**
   * Select file by file path and filter.
   *
   * @param path the file path to be scanned, the scanning can scan all files under this path.
   * @param filter the file filter
   * @return selectFiles
   */
  protected List<File> selectFiles(String path, Predicate<File> filter) {
    final File root = new File(path);
    final Predicate<File> predicate = defaultObject(filter, Functions.emptyPredicate(true));
    List<File> files = new ArrayList<>();
    if (root.exists()) {
      LinkedList<File> candidates = linkedListOf(root);
      File candidate;
      while ((candidate = candidates.poll()) != null) {
        if (candidate.isFile()) {
          if (predicate.test(candidate)) {
            files.add(candidate);
          }
        } else {
          for (File file : defaultObject(candidate.listFiles(), FileUtils.EMPTY_ARRAY)) {
            candidates.offer(file);
          }
        }
      }
    }
    return files;
  }
}
