package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

public interface MojoMetadata {
  MavenProject getProject();

  Log getLog();

  String getEncoding();

  BuildContext getBuildContext();
}
