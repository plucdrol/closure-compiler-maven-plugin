package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHelper {
  private FileHelper() {}

  public static String relativizePath(File base, File target) throws IOException {
    // Resolve source map path relative to source file
    base = base.isDirectory() ? base : base.getParentFile();
    final Path basePath = base.getCanonicalFile().toPath();
    final Path targetPath = Paths.get(target.getCanonicalPath());
    final String relativePath = basePath.relativize(targetPath).toString();
    return relativePath;
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
