package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;

public class FilenameInterpolator implements UnaryOperator<File> {
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

  @Override
  public File apply(File inputFile) {
    return interpolate(inputFile, null);
  }

  public File interpolate(File inputFile, Map<String, String> additionalData) {
    String inputFilename = inputFile.getName();
    Map<String, String> data = new HashMap<>();
    data.put("filename", inputFilename);
    data.put("extension", FilenameUtils.getExtension(inputFilename));
    data.put("basename", FilenameUtils.getBaseName(inputFilename));
    if (additionalData != null) {
      data.putAll(additionalData);
    }
    StringSubstitutor substitutor = new StringSubstitutor(data, prefix, suffix, escapeChar);
    String sourceMapFilename = substitutor.replace(pattern);
    File outputFile = new File(inputFile.getParentFile(), sourceMapFilename);
    return outputFile;
  }
}
