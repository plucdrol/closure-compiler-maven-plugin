/*
 * Closure Compiler Maven Plugin https://github.com/blutorange/closure-compiler-maven-plugin Original license terms
 * below. Changes were made to this file.
 */

/*
 * Minify Maven Plugin https://github.com/samaxes/minify-maven-plugin Copyright (c) 2009 samaxes.com Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileException;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileProcessConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSpecifier;
import com.github.blutorange.maven.plugin.closurecompiler.common.FilenameInterpolator;
import com.github.blutorange.maven.plugin.closurecompiler.common.SourceFilesEnumeration;
import com.github.blutorange.maven.plugin.closurecompiler.common.TwoTuple;
import com.google.common.base.Predicate;

/**
 * Abstract class for merging and compressing a files list.
 */
public abstract class ProcessFilesTask implements Callable<Object> {

  private static final String DEFAULT_MERGED_FILENAME = "script.js";

  public static final String TEMP_SUFFIX = ".tmp";

  /**
   * Logs an addition of a new source file.
   * @param finalFilename the final file name
   * @param sourceFile the source file
   * @throws IOException
   */
  private static void addNewSourceFile(Collection<File> files, File sourceFile, MojoMetadata mojoMeta) throws IOException {
    if (sourceFile.exists()) {
      mojoMeta.getLog().debug("Adding source file [" + sourceFile.getPath() + "].");
      files.add(sourceFile.getCanonicalFile());
    }
    else {
      throw new FileNotFoundException("The source file [" + sourceFile.getPath() + "] does not exist.");
    }
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(keyExtractor.apply(t));
  }

  /**
   * Returns the files to copy. Default exclusions are used when the excludes list is empty.
   * @param includes list of source files to include
   * @param excludes list of source files to exclude
   * @return the files to copy
   */
  private static List<File> getFilesToInclude(File sourceDir, List<String> includes, List<String> excludes) {
    if (includes == null || includes.isEmpty()) { return new ArrayList<>(); }

    String[] excludesArray = excludes.toArray(new String[excludes.size()]);

    // For each specified include, get all matching files, then
    // sort first by the specified order, then by file path. Finally,
    // filter out duplicate files.
    return IntStream.range(0, includes.size()) //
        .mapToObj(i -> new TwoTuple<>(i, includes.get(i))) //
        .flatMap(include -> {
          DirectoryScanner scanner = new DirectoryScanner();
          scanner.setIncludes(new String[] { include.getSecond() });
          scanner.setExcludes(excludesArray);
          scanner.addDefaultExcludes();
          scanner.setBasedir(sourceDir);
          scanner.scan();
          return Arrays.stream(scanner.getIncludedFiles()).map(includedFilename -> {
            File includedFile = new File(sourceDir, includedFilename);
            return new TwoTuple<>(include.getFirst(), includedFile);
          });
        }) //
        .sorted(TwoTuple.getComparator()) //
        .map(TwoTuple::getSecond) //
        .filter(distinctByKey(File::getAbsolutePath)) //
        .collect(Collectors.toList());
  }

  protected final MojoMetadata mojoMeta;

  protected final ClosureConfig closureConfig;

  protected final List<File> files = new ArrayList<>();

  protected final boolean includesEmpty;

  protected final FilenameInterpolator outputFilenameInterpolator;

  protected final FileProcessConfig processConfig;

  protected final File sourceDir;

  protected final File targetDir;

  /**
   * Task constructor.
   * @param mojoMeta Base mojo data.
   * @param bufferSize size of the buffer used to read source files
   * @param force Whether the check for changed files is skipped.
   * @param skipMerge whether to skip the merge step or not
   * @param skipMinify whether to skip the minify step or not
   * @param fileSpecifier Details about the input / output files.
   * @param closureConfig Google closure configuration
   * @throws IOException
   */
  public ProcessFilesTask(MojoMetadata mojoMeta, FileProcessConfig processConfig, FileSpecifier fileSpecifier, ClosureConfig closureConfig) throws IOException {
    this.mojoMeta = mojoMeta;
    this.processConfig = processConfig;

    File projectBasedir = mojoMeta.getProject().getBasedir();
    this.sourceDir = FileUtils.getFile(FileHelper.getAbsoluteFile(projectBasedir, fileSpecifier.getBaseSourceDir()), fileSpecifier.getSourceDir()).getAbsoluteFile().getCanonicalFile();
    this.targetDir = FileUtils.getFile(FileHelper.getAbsoluteFile(projectBasedir, fileSpecifier.getBaseTargetDir()), fileSpecifier.getTargetDir()).getAbsoluteFile().getCanonicalFile();
    this.outputFilenameInterpolator = new FilenameInterpolator(fileSpecifier.getOutputFilename());

    for (File include : getFilesToInclude(this.sourceDir, fileSpecifier.getIncludes(), fileSpecifier.getExcludes())) {
      if (!files.contains(include)) {
        addNewSourceFile(files, include, mojoMeta);
      }
    }

    this.includesEmpty = fileSpecifier.getIncludes().isEmpty();
    this.closureConfig = closureConfig;
  }

  private void assertTarget(File source, File target) throws MojoFailureException {
    if (target.getAbsolutePath().equals(source.getAbsolutePath())) {
      String msg;
      msg = "The source file [" + source.getName() + "] has the same name as the output file [" + target.getName() + "].";
      mojoMeta.getLog().warn(msg);
      mojoMeta.getLog().debug("Full path for source file is [" + source.getPath() + "] and for target file [" + target.getPath() + "]");
      throw new MojoFailureException(msg);
    }
  }

  /**
   * Method executed by the thread.
   * @throws IOException when the merge or minify steps fail
   * @throws MojoFailureException
   */
  @Override
  public Object call() throws IOException, MojoFailureException {
    synchronized (mojoMeta.getLog()) {
      mojoMeta.getLog().info("Starting JavaScript task:");
      if (!files.isEmpty()) {
        try {
          processFiles();
        }
        catch (FileException e) {
          e.getFileErrors().forEach(fileError -> fileError.addTo(mojoMeta.getBuildContext()));
          throw new MojoFailureException("Closure compilation failure", e);
        }
        catch (Exception e) {
          files.forEach(file -> {
            mojoMeta.getBuildContext().addMessage(file, 1, 1, e.getMessage(), BuildContext.SEVERITY_ERROR, e);
          });
          throw e;
        }
      }
      else if (!includesEmpty) {
        // 'files' list will be empty if source file paths or names added to the project's POM are invalid.
        mojoMeta.getLog().error("No valid JavaScript source files found to process.");
      }
    }

    return null;
  }

  /**
   * Copy, merge and / or minify the input files.
   * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
   * change).
   * @throws IOException
   * @throws MojoFailureException
   */
  private void processFiles() throws IOException, MojoFailureException {
    // Minify only
    if (processConfig.isSkipMerge()) {
      mojoMeta.getLog().info("Skipping the merge step...");

      for (File sourceFile : files) {
        // Create folders to preserve sub-directory structure when only minifying / copying
        File minifiedFile = outputFilenameInterpolator.apply(sourceFile, targetDir);
        assertTarget(sourceFile, minifiedFile);
        if (processConfig.isSkipMinify()) {
          copy(sourceFile, minifiedFile);
        }
        else {
          minify(sourceFile, minifiedFile);
        }
      }
    }
    // Merge-only
    else if (processConfig.isSkipMinify()) {
      File mergedFile = outputFilenameInterpolator.apply(new File(targetDir, DEFAULT_MERGED_FILENAME), targetDir);
      merge(mergedFile);
      mojoMeta.getLog().info("Skipping the minify step...");
    }
    // Minify + merge
    else {
      File minifiedFile = outputFilenameInterpolator.apply(new File(targetDir, DEFAULT_MERGED_FILENAME), targetDir);
      minify(files, minifiedFile);
    }
  }

  protected void removeMessages(Collection<File> files) {
    files.forEach(file -> mojoMeta.getBuildContext().removeMessages(file));
  }

  /**
   * Logs compression gains.
   * @param srcFiles list of input files to compress
   * @param minifiedFile output file resulting from the minify step
   */
  protected void logCompressionGains(List<File> srcFiles, String minified) {
    if (!mojoMeta.getLog().isInfoEnabled() || mojoMeta.getBuildContext().isIncremental()) { return; }
    try {
      byte[] minifiedData = minified.getBytes(mojoMeta.getEncoding());
      long compressedSize = minifiedData.length;
      long compressedSizeGzip;

      try (InputStream in = new ByteArrayInputStream(minifiedData);
          CountingOutputStream out = new CountingOutputStream(new NullOutputStream());
          GZIPOutputStream outGZIP = new GZIPOutputStream(out)) {
        IOUtils.copy(in, outGZIP, processConfig.getBufferSize());
        outGZIP.finish();
        compressedSizeGzip = out.getByteCount();
      }

      long uncompressedSize = 0;
      if (srcFiles != null) {
        for (File srcFile : srcFiles) {
          uncompressedSize += srcFile.length();
        }
      }

      mojoMeta.getLog().info("Uncompressed size: " + uncompressedSize + " bytes.");
      mojoMeta.getLog().info("Compressed size: " + compressedSize + " bytes minified (" + compressedSizeGzip + " bytes gzipped).");
    }
    catch (IOException e) {
      mojoMeta.getLog().debug("Failed to calculate the gzipped file size.", e);
    }
  }

  /**
   * Creates the given directory (and parents) and informs the build context.
   * @param directory
   */
  protected void mkDir(File directory) {
    if (directory.exists()) { return; }
    File firstThatExists = directory;
    do {
      firstThatExists = firstThatExists.getParentFile();
    }
    while (firstThatExists != null && !firstThatExists.exists());
    try {
      if (!directory.mkdirs()) { throw new RuntimeException("Unable to create target directory: " + directory.getPath()); }
    }
    finally {
      if (firstThatExists != null) {
        mojoMeta.getBuildContext().refresh(firstThatExists);
      }
    }
  }

  /**
   * Copies sourceFile to targetFile, making sure to inform the build context of the change.
   * @param sourceFile
   * @param targetFile
   * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
   * change).
   * @throws IOException
   */
  protected boolean copy(File sourceFile, File targetFile) throws IOException {
    if (!haveFilesChanged(Collections.singleton(sourceFile), Collections.singleton(targetFile))) { return false; }
    mkDir(targetDir);
    mkDir(targetFile.getParentFile());
    try (InputStream in = new FileInputStream(sourceFile);
        OutputStream out = mojoMeta.getBuildContext().newFileOutputStream(targetFile);
        Reader reader = new InputStreamReader(in, mojoMeta.getEncoding());
        Writer writer = new OutputStreamWriter(out, mojoMeta.getEncoding())) {
      IOUtils.copy(reader, writer);
    }
    mojoMeta.getLog().info("Creating the copied file [" + targetFile.getName() + "].");
    mojoMeta.getLog().debug("Full path is [" + targetFile.getPath() + "].");

    return true;
  }

  /**
   * Merges a list of source files. Create missing parent directories if needed.
   * @param mergedFile output file resulting from the merged step
   * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
   * change).
   * @throws IOException when the merge step fails
   */
  protected void merge(File mergedFile) throws IOException {
    if (!haveFilesChanged(files, Collections.singleton(mergedFile))) { return; }

    mkDir(targetDir);
    mkDir(mergedFile.getParentFile());

    try (InputStream sequence = new SequenceInputStream(new SourceFilesEnumeration(mojoMeta.getLog(), files, mojoMeta.getEncoding(), processConfig.getLineSeparator()));
        OutputStream out = mojoMeta.getBuildContext().newFileOutputStream(mergedFile);
        InputStreamReader sequenceReader = new InputStreamReader(sequence, mojoMeta.getEncoding());
        OutputStreamWriter outWriter = new OutputStreamWriter(out, mojoMeta.getEncoding())) {
      mojoMeta.getLog().info("Creating the merged file [" + mergedFile.getName() + "].");
      mojoMeta.getLog().debug("Full path is [" + mergedFile.getPath() + "].");

      IOUtils.copyLarge(sequenceReader, outWriter, new char[processConfig.getBufferSize()]);

      // Make sure we end with a new line
      outWriter.append(processConfig.getLineSeparator());
    }
  }

  /**
   * @param sourceFiles
   * @param outputFiles
   * @return Whether any change was made to the source / output files. If not, then we can skip the execution of the
   * current bundle.
   */
  protected boolean haveFilesChanged(Collection<File> sourceFiles, Collection<File> outputFiles) {
    boolean changed;
    if (processConfig.isForce() && mojoMeta.getBuildContext().isIncremental()) {
      mojoMeta.getLog().warn("Force is enabled, but building incrementally. Using the force option in an m2e incremental build will result in an endless build loop.");
    }

    if (processConfig.isForce()) {
      mojoMeta.getLog().debug("Force is enabled, skipping check for changed files.");
      changed = true;
    }
    else {
      changed = sourceFiles.stream().anyMatch(mojoMeta.getBuildContext()::hasDelta);
    }
    if (changed) {
      removeMessages(sourceFiles);
    }
    if (mojoMeta.getLog().isDebugEnabled()) {
      String prefix = changed ? "Changes since last build, processing bundle with output files [" : "No changes since last build, skipping bundle with output files [";
      mojoMeta.getLog().debug(prefix + outputFiles.stream().map(File::getPath).collect(Collectors.joining(", ")) + "].");
    }
    return changed;
  }

  /**
   * Minifies a source file. Create missing parent directories if needed.
   * @param mergedFile input file resulting from the merged step
   * @param minifiedFile output file resulting from the minify step
   * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
   * change).
   * @throws IOException when the minify step fails
   * @throws MojoFailureException
   */
  abstract void minify(File mergedFile, File minifiedFile) throws IOException, MojoFailureException;

  /**
   * Minifies a list of source files into a single file. Create missing parent directories if needed.
   * @param srcFiles list of input files
   * @param minifiedFile output file resulting from the minify step
   * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
   * change).
   * @throws IOException when the minify step fails
   * @throws MojoFailureException
   */
  abstract void minify(List<File> srcFiles, File minifiedFile) throws IOException, MojoFailureException;
}
