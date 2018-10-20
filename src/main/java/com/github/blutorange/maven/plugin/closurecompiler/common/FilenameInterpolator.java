package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;

public class FilenameInterpolator implements BinaryOperator<File> {
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
  public File apply(File inputFile, File targetDirectory) {
    return interpolate(inputFile, targetDirectory, null);
  }

  public File interpolate(File inputFile, File targetDirectory, Map<String, String> additionalData) {
    String inputFilename = inputFile.getName();
    Map<String, String> data = new HashMap<>();
    data.put("filename", inputFilename);
    data.put("extension", FilenameUtils.getExtension(inputFilename));
    data.put("basename", FilenameUtils.getBaseName(inputFilename));
    if (additionalData != null) {
      data.putAll(additionalData);
    }
    StringSubstitutor substitutor = new StringSubstitutor(data, prefix, suffix, escapeChar);
    String interpolatedFilename = substitutor.replace(pattern);
    File interpolatedFile = new File(targetDirectory, interpolatedFilename);
    return interpolatedFile;
  }
}
