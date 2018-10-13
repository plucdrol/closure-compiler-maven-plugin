package com.github.blutorange.maven.plugin.closurecompiler.common;

public enum SourceMapOutputType {
  /** Just create a source map file named *.map. */
  file,
  /** Inline the content of the source map in the minified file. */
  inline,
  /** Create a source map file *.map and add a reference to it in the minified file. */
  reference
}
