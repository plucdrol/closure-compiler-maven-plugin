package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
  private FileUtil() {}

  public static String relativizePath(File base, File target) throws IOException {
    // Resolve source map path relative to source file
    final Path basePath = Paths.get(base.getCanonicalPath()).getParent();
    final Path targetPath = Paths.get(target.getCanonicalPath());
    final String relativePath = basePath.relativize(targetPath).toString();
    return relativePath;
  }
}
