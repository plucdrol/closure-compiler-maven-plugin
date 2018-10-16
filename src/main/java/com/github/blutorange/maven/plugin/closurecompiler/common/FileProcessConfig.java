package com.github.blutorange.maven.plugin.closurecompiler.common;

public class FileProcessConfig {
  private final boolean skipMerge;
  private final boolean skipMinify;
  private final boolean force;
  private final int bufferSize;
  private final String lineSeparator;

  public FileProcessConfig(String lineSeparator, int bufferSize, boolean force,
      boolean skipMerge, boolean skipMinify) {
    this.lineSeparator = lineSeparator;
    this.bufferSize = bufferSize;
    this.force = force;
    this.skipMerge = skipMerge;
    this.skipMinify = skipMerge;
  }

  public boolean isSkipMerge() {
    return skipMerge;
  }

  public boolean isSkipMinify() {
    return skipMinify;
  }

  public boolean isForce() {
    return force;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public String getLineSeparator() {
    return lineSeparator;
  }
}
