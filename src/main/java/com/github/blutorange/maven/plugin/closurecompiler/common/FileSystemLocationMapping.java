package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;

import com.google.javascript.jscomp.SourceMap.LocationMapping;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Makes the source file relative to the location of the source map. The paths are taken from the
 * file system.
 */
public class FileSystemLocationMapping implements LocationMapping {
    private final File baseDirForSourceFiles;
    private final File minifiedFile;
    private final File sourceMapDir;
    private final Log log;
    private boolean transpilationDone;
    public FileSystemLocationMapping(Log log, File baseDirForSourceFiles, File minifiedFile, File sourceMapFile) {
        this.log = log;
        this.minifiedFile = minifiedFile;
        this.baseDirForSourceFiles = baseDirForSourceFiles;
        this.sourceMapDir = sourceMapFile.getParentFile();
    }
    @Override
    public String map(String location) {
      try {
        final String mapped;
        if (transpilationDone) {
          // This is the source file relative to the source map
          mapped = location;
        }
        else {
          final File file = new File(baseDirForSourceFiles, location);
          mapped = FilenameUtils.separatorsToUnix(FileHelper.relativizePath(sourceMapDir, file));
        }
        log.debug("Source map: mapping location [" + location + "] to [" + mapped + "]");
        return mapped;
      }
      catch (IOException e) {
        log.error("Could not map source location", e);
        return null;
      }
    }
	public void setTranspilationDone(boolean transpilationDone) {
    this.transpilationDone = transpilationDone;
	}
}