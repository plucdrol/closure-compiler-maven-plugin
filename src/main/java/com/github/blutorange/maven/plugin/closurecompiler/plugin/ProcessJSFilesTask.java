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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.LightweightMessageFormatter;
import com.google.javascript.jscomp.MessageFormatter;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;

import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlBuilder;
import eu.maxschuster.dataurl.DataUrlEncoding;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;

/**
 * Task for merging and compressing JavaScript files.
 */
public class ProcessJSFilesTask extends ProcessFilesTask {

  /**
   * Task constructor.
   * @param mojoMeta Mojo meta (for log, project etc.)
   * @param verbose display additional info
   * @param bufferSize size of the buffer used to read source files
   * @param encoding if a character set is specified, a byte-to-char variant allows the encoding to be selected.
   * Otherwise, only byte-to-byte operations are used
   * @param skipMerge whether to skip the merge step or not
   * @param skipMinify whether to skip the minify step or not
   * @param baseSourceDir web resources source directory
   * @param baseTargetDir web resources target directory
   * @param sourceDir directory containing source files
   * @param includes list of source files to include
   * @param excludes list of source files to exclude
   * @param targetDir directory to write the final file
   * @param outputFilename the output file name
   * @param closureConfig Google Closure Compiler configuration
   * @throws IOException
   */
  public ProcessJSFilesTask(MojoMetadata mojoMeta, int bufferSize,
      boolean skipMerge, boolean skipMinify,
      File baseSourceDir, File baseTargetDir,
      String sourceDir, String targetDir,
      List<String> includes, List<String> excludes,
      String outputFilename, ClosureConfig closureConfig)
      throws IOException {
    super(mojoMeta, bufferSize, skipMerge, skipMinify, baseSourceDir,
        baseTargetDir, sourceDir, targetDir, includes, excludes, outputFilename,
        closureConfig);
  }

  /**
   * Minifies a JavaScript file. Create missing parent directories if needed.
   * @param mergedFile input file resulting from the merged step
   * @param minifiedFile output file resulting from the minify step
   * @throws IOException when the minify step fails
   */
  @Override
  protected void minify(File mergedFile, File minifiedFile) throws IOException {
    List<File> srcFiles = new ArrayList<File>();
    srcFiles.add(mergedFile);
    minify(srcFiles, minifiedFile);
  }

  @Override
  protected void minify(List<File> srcFiles, File minifiedFile) throws IOException {
    minifiedFile.getParentFile().mkdirs();

    UnaryOperator<String> outputInterpolator = closureConfig.getOutputInterpolator();

    try (OutputStream out = new FileOutputStream(minifiedFile); Writer writer = new OutputStreamWriter(out, encoding)) {

      log.info("Creating the minified file [" + ((verbose) ? minifiedFile.getPath() : minifiedFile.getName())
          + "].");

      log.debug("Using Google Closure Compiler engine.");

      // List<SourceFile> sourceFileList = srcFiles.stream().map(f -> SourceFile.fromPath(f.toPath(),
      // charset)).collect(Collectors.toList());
      List<SourceFile> sourceFileList = new ArrayList<SourceFile>();
      for (File srcFile : srcFiles) {
        InputStream in = new FileInputStream(srcFile);
        SourceFile input = SourceFile.fromInputStream(srcFile.getName(), in, encoding);
        sourceFileList.add(input);
      }

      // Set common options
      File sourceMapFile = closureConfig.getSourceMapInterpolator().apply(minifiedFile);
      CompilerOptions options = closureConfig.getCompilerOptions(sourceMapFile);

      // Set (external) libraries to be available
      List<SourceFile> externs = new ArrayList<>();
      externs.addAll(CommandLineRunner.getBuiltinExterns(closureConfig.getEnvironment()));
      externs.addAll(closureConfig.getExterns());

      // Now compile
      final Compiler compiler = new Compiler();
      compiler.compile(externs, sourceFileList, options);

      // Check for errors.
      checkForErrors(compiler);

      // Write compiled file to output file
      writer.append(outputInterpolator.apply(compiler.toSource()));

      // Create source map if configured.
      if (closureConfig.isCreateSourceMap()) {
        createSourceMap(writer, compiler, minifiedFile, sourceMapFile);
      }
    }
    catch (IOException e) {
      log.error(
          "Failed to compress the JavaScript file ["
              + (verbose ? minifiedFile.getPath() : minifiedFile.getName()) + "].",
          e);
      throw e;
    }

    logCompressionGains(srcFiles, minifiedFile);
  }

  private void checkForErrors(Compiler compiler) {
    JSError[] errors = compiler.getErrors();
    if (errors.length > 0) {
      StringBuilder msg = new StringBuilder("JSCompiler errors\n");
      MessageFormatter formatter = new LightweightMessageFormatter(compiler);
      for (JSError e : errors) {
        msg.append(formatter.formatError(e));
      }
      throw new RuntimeException(msg.toString());
    }
  }

  private void createSourceMap(Writer writer, Compiler compiler, File minifiedFile, File sourceMapFile) throws IOException {
    log.info("Creating the minified files map ["
        + ((verbose) ? sourceMapFile.getPath() : sourceMapFile.getName()) + "].");

    switch (closureConfig.getSourceMapOutputType()) {
      case inline:
        StringBuilder sb = new StringBuilder();
        compiler.getSourceMap().appendTo(sb, minifiedFile.getName());
        DataUrl unserialized = new DataUrlBuilder() //
            .setMimeType("application/json") //
            .setEncoding(DataUrlEncoding.BASE64) //
            .setData(sb.toString().getBytes(StandardCharsets.UTF_8)) //
            .setHeader("charset", "utf-8") //
            .build();
        IDataUrlSerializer serializer = new DataUrlSerializer();
        String dataUrl = serializer.serialize(unserialized);
        writer.append(System.getProperty("line.separator"));
        writer.append("//# sourceMappingURL=" + dataUrl);
        break;
      case file:
        sourceMapFile.createNewFile();
        flushSourceMap(sourceMapFile, minifiedFile.getName(), compiler.getSourceMap());
        break;
      case reference:
        sourceMapFile.createNewFile();
        flushSourceMap(sourceMapFile, minifiedFile.getName(), compiler.getSourceMap());
        writer.append(System.getProperty("line.separator"));
        writer.append("//# sourceMappingURL=" + sourceMapFile.getName());
        break;
      default:
        log.warn("unknown source map inclusion type: " + closureConfig.getSourceMapOutputType());
        throw new RuntimeException("unknown source map inclusion type: " + closureConfig.getSourceMapOutputType());
    }
  }

  private boolean flushSourceMap(File sourceMapFile, String minifyFileName, SourceMap sourceMap) {
    try (BufferedWriter out = Files.newBufferedWriter(sourceMapFile.toPath(), StandardCharsets.UTF_8)) {
      sourceMap.appendTo(out, minifyFileName);
      return true;
    }
    catch (IOException e) {
      log.error("Failed to write the JavaScript Source Map file ["
          + (verbose ? sourceMapFile.getPath() : sourceMapFile.getName()) + "].", e);
      return false;
    }
  }
}