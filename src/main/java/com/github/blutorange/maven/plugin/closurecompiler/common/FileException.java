package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class FileException extends RuntimeException {
  private final Iterable<FileMessage> fileErrors;

  public FileException(Iterable<FileMessage> fileErrors) {
    this.fileErrors = fileErrors != null ? fileErrors : new ArrayList<>();
  }

  public Iterable<FileMessage> getFileErrors() {
    return fileErrors;
  }
}
