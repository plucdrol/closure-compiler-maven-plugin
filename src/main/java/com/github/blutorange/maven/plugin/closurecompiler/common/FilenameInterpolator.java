package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;

public class FilenameInterpolator {
  private final String pattern;
  private final String prefix;
  private final String suffix;
  private final char escapeChar;

  public FilenameInterpolator(String pattern) {
    this(pattern, "#{", "}", '#');
  }

  public FilenameInterpolator(String pattern, String prefix, String suffix, char escapeChar) {
    this.pattern = pattern;
    this.prefix = prefix;
    this.suffix = suffix;
    this.escapeChar = escapeChar;
  }

  public File interpolate(File inputFile, File inputBaseDir, File targetDirectory) throws IOException {
    return interpolate(inputFile, inputBaseDir, targetDirectory, null);
  }

  public File interpolate(File inputFile, File inputBaseDir, File targetDirectory, Map<String, String> additionalData) throws IOException {
    String inputFilename = inputFile.getName();
    Map<String, String> data = new HashMap<>();
    data.put("filename", inputFilename);
    data.put("extension", FilenameUtils.getExtension(inputFilename));
    data.put("basename", FilenameUtils.getBaseName(inputFilename));
    data.put("path", FileHelper.relativizePath(inputBaseDir, inputFile.getParentFile()));
    if (additionalData != null) {
      data.putAll(additionalData);
    }
    StringSubstitutor substitutor = new StringSubstitutor(data, prefix, suffix, escapeChar);
    String interpolatedFilename = substitutor.replace(pattern);
    File interpolatedFile = new File(targetDirectory, interpolatedFilename);
    return interpolatedFile;
  }
}
