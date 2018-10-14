package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public interface MojoMetadata {
  MavenProject getProject();

  Log getLog();

  String getEncoding();

  boolean isVerbose();
}
