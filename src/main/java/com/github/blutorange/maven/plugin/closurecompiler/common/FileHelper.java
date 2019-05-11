package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class FileHelper {
  private FileHelper() {}

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
   * @param file File to make absolute.
   */
  public static File getAbsoluteFile(File basedir, File file) {
    if (file.isAbsolute()) { return file; }
    return new File(basedir, file.getPath());
  }

  public static File getAbsoluteFile(File basedir, String file) {
    return getAbsoluteFile(basedir, new File(file));
  }
}
