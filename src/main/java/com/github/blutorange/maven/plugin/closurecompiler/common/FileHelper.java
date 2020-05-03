package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.DirectoryScanner;

public class FileHelper {
  private FileHelper() {
  }

  /**
   * @return The path of the given {@code target}, relative to the specified {@code base} file.
   */
  public static String relativizePath(File base, File target) throws IOException {
    final Path targetPath = Paths.get(target.getCanonicalPath());
    if (base == null) {
      return targetPath.toString();
    }
    else {
      final Path basePath = base.getCanonicalFile().toPath();
      final String relativePath = basePath.relativize(targetPath).toString();
      return relativePath;
    }
  }

  /**
   * Sames as {@link FileUtils#getFile(File, String...)}, but ignores empty names.
   */
  public static File getFile(final File directory, final String... names) {
    return FileUtils.getFile(directory, Arrays.stream(names).filter(StringUtils::isNotEmpty).toArray(String[]::new));
  }

  /**
   * If the file does not refer to an absolute path, return the file relative to the given basedir. If the file already
   * refers to an absolute path, return the file.
   * @param basedir Directory to which the file is relative.
   * @param file    File to make absolute.
   */
  public static File getAbsoluteFile(File basedir, File file) {
    if (file.isAbsolute()) { return file; }
    return new File(basedir, file.getPath());
  }

  public static File getAbsoluteFile(File basedir, String file) {
    return getAbsoluteFile(basedir, new File(file));
  }

  /**
   * Processes the includes and excludes relative to the given base directory, and returns all included files.
   * @param baseDir Base directory of the includes and excludes.
   * @param includes List of specified includes
   * @param excludes List of specified excludes
   * @return A list of all files matching the given includes and excludes.
   */
  public static List<File> getIncludedFiles(File baseDir, List<String> includes, List<String> excludes) {
    if (CollectionUtils.isEmpty(includes)) {
      return new ArrayList<>(); 
    }
    String[] excludesArray = excludes.toArray(new String[excludes.size()]);

    return IntStream.range(0, includes.size()) //
        .mapToObj(i -> Pair.of(i, includes.get(i))) //
        .flatMap(include -> {
          DirectoryScanner scanner = new DirectoryScanner();
          scanner.setIncludes(new String[] { include.getRight() });
          scanner.setExcludes(excludesArray);
          scanner.addDefaultExcludes();
          scanner.setBasedir(baseDir);
          scanner.scan();
          return Arrays.stream(scanner.getIncludedFiles()).map(includedFilename -> {
            File includedFile = new File(baseDir, includedFilename);
            return Pair.of(include.getLeft(), includedFile);
          });
        }) //
        .sorted() //
        .map(Pair::getRight) //
        .filter(distinctByKey(File::getAbsolutePath)) //
        .collect(Collectors.toList());
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
