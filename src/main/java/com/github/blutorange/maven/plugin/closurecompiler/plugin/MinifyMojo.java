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

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.blutorange.maven.plugin.closurecompiler.common.Aggregation;
import com.github.blutorange.maven.plugin.closurecompiler.common.AggregationConfiguration;
import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.DependencyOptions;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.SourceFile;

/**
 * Goal for combining and/or minifying JavaScript files with closure compiler.
 */
@Mojo(name = "minify", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class MinifyMojo extends AbstractMojo {

  /* ************** */
  /* Global Options */
  /* ************** */

  /**
   * Display additional informational messages and warnings.
   */
  @Parameter(property = "verbose", defaultValue = "false")
  private boolean verbose;

  /**
   * Size of the buffer used to read source files.
   */
  @Parameter(property = "bufferSize", defaultValue = "4096")
  private int bufferSize;

  /**
   * If a supported character set is specified, it will be used to read the input file. Otherwise, it will assume that
   * the platform's default character set is being used. The output file is encoded using the same character set.<br/>
   * See the <a href="http://www.iana.org/assignments/character-sets">IANA Charset Registry</a> for a list of valid
   * encoding types.
   * @since 1.3.2
   */
  @Parameter(property = "charset", defaultValue = "${project.build.sourceEncoding}")
  private String charset;

  /**
   * The output file name suffix.
   * @since 1.3.2
   */
  @Parameter(property = "suffix", defaultValue = ".min")
  private String suffix;

  /**
   * Do not append a suffix to the minified output file name, independently of the value in the {@code suffix}
   * parameter.<br/>
   * <strong>Warning:</strong> when both the options {@code nosuffix} and {@code skipMerge} are set to {@code true}, the
   * plugin execution phase needs to be set to {@code package}, otherwise the output files will be overridden by the
   * source files during the packaging.
   * @since 1.7
   */
  @Parameter(property = "nosuffix", defaultValue = "false")
  private boolean nosuffix;

  /**
   * Skip the merge step. Minification will be applied to each source file individually.
   * @since 1.5.2
   */
  @Parameter(property = "skipMerge", defaultValue = "false")
  private boolean skipMerge;

  /**
   * Skip the minify step. Useful when merging files that are already minified.
   * @since 1.5.2
   */
  @Parameter(property = "skipMinify", defaultValue = "false")
  private boolean skipMinify;

  /**
   * Webapp source directory.
   */
  @Parameter(property = "webappSourceDir", defaultValue = "${basedir}/src/main/webapp")
  private String webappSourceDir;

  /**
   * Webapp target directory.
   */
  @Parameter(property = "webappTargetDir", defaultValue = "${project.build.directory}/${project.build.finalName}")
  private String webappTargetDir;

  /**
   * Specify aggregations in an external JSON formatted config file.
   * @since 1.7.5
   */
  @Parameter(property = "bundleConfiguration")
  private String bundleConfiguration;

  /* ****************** */
  /* JavaScript Options */
  /* ****************** */

  /**
   * JavaScript source directory.
   */
  @Parameter(property = "jsSourceDir", defaultValue = "js")
  private String jsSourceDir;

  /**
   * JavaScript source file names list.
   */
  @Parameter(property = "jsSourceFiles", alias = "jsFiles")
  private ArrayList<String> jsSourceFiles;

  /**
   * JavaScript files to include. Specified as fileset patterns which are relative to the JavaScript source directory.
   * @since 1.2
   */
  @Parameter(property = "jsSourceIncludes", alias = "jsIncludes")
  private ArrayList<String> jsSourceIncludes;

  /**
   * JavaScript files to exclude. Specified as fileset patterns which are relative to the JavaScript source directory.
   * @since 1.2
   */
  @Parameter(property = "jsSourceExcludes", alias = "jsExcludes")
  private ArrayList<String> jsSourceExcludes;

  /**
   * JavaScript target directory. Takes the same value as {@code jsSourceDir} when empty.
   * @since 1.3.2
   */
  @Parameter(property = "jsTargetDir")
  private String jsTargetDir;

  /**
   * JavaScript output file name.
   */
  @Parameter(property = "jsFinalFile", defaultValue = "script.js")
  private String jsFinalFile;

  /* ************************************ */
  /* Google Closure Compiler Only Options */
  /* ************************************ */

  /**
   * Refers to which version of ECMAScript to assume when checking for errors in your code.<br/>
   * Possible values are:
   * <ul>
   * <li>{@code ECMASCRIPT3}: Checks code assuming ECMAScript 3 compliance, and gives errors for code using features
   * only present in later versions of ECMAScript.</li>
   * <li>{@code ECMASCRIPT5}: Checks code assuming ECMAScript 5 compliance, allowing new features not present in
   * ECMAScript 3, and gives errors for code using features only present in later versions of ECMAScript.</li>
   * <li>{@code ECMASCRIPT5_STRICT}: Like {@code ECMASCRIPT5} but assumes compliance with strict mode
   * ({@code 'use strict';}).</li>
   * <li>{@code ECMASCRIPT_2015}: Checks code assuming ECMAScript 2015 compliance.</li>
   * <li>{@code ECMASCRIPT6_TYPED}: Checks code assuming a superset of ECMAScript 6 which adds Typescript-style type
   * declarations.</li>
   * <li>{@code ECMASCRIPT_2016}: Checks code assuming ECMAScript 2016 compliance.</li>
   * <li>{@code ECMASCRIPT_2017}: Checks code assuming ECMAScript 2017 compliance.</li>
   * <li>{@code ECMASCRIPT_NEXT}: Checks code assuming ECMAScript latest draft standard.</li>
   * </ul>
   * @since 1.7.2
   */
  @Parameter(property = "closureLanguageIn", defaultValue = "ECMASCRIPT_2016")
  private LanguageMode closureLanguageIn;

  /**
   * Refers to which version of ECMAScript your code will be returned in.<br/>
   * It accepts the same options as {@code closureLanguageIn} and is used to transpile between different levels of
   * ECMAScript.
   * @since 1.7.5
   */
  @Parameter(property = "closureLanguageOut", defaultValue = "ECMASCRIPT_2015")
  private LanguageMode closureLanguageOut;

  /**
   * Determines the set of builtin externs to load.<br/>
   * Options: BROWSER, CUSTOM.
   * @since 1.7.5
   */
  @Parameter(property = "closureEnvironment", defaultValue = "BROWSER")
  private CompilerOptions.Environment closureEnvironment;

  /**
   * The degree of compression and optimization to apply to your JavaScript.<br/>
   * There are three possible compilation levels:
   * <ul>
   * <li>{@code WHITESPACE_ONLY}: Just removes whitespace and comments from your JavaScript.</li>
   * <li>{@code SIMPLE_OPTIMIZATIONS}: Performs compression and optimization that does not interfere with the
   * interaction between the compiled JavaScript and other JavaScript. This level renames only local variables.</li>
   * <li>{@code ADVANCED_OPTIMIZATIONS}: Achieves the highest level of compression by renaming symbols in your
   * JavaScript. When using {@code ADVANCED_OPTIMIZATIONS} compilation you must perform extra steps to preserve
   * references to external symbols. See <a href="/closure/compiler/docs/api-tutorial3">Advanced Compilation and
   * Externs</a> for more information about {@code ADVANCED_OPTIMIZATIONS}.</li>
   * </ul>
   * @since 1.7.2
   */
  @Parameter(property = "closureCompilationLevel", defaultValue = "SIMPLE_OPTIMIZATIONS")
  private CompilationLevel closureCompilationLevel;

  /**
   * List of JavaScript files containing code that declares function names or other symbols. Use {@code closureExterns}
   * to preserve symbols that are defined outside of the code you are compiling. The {@code closureExterns} parameter
   * only has an effect if you are using a {@code CompilationLevel} of {@code ADVANCED_OPTIMIZATIONS}.<br/>
   * These file names are relative to {@link #webappSourceDir} directory.
   * @since 1.7.2
   */
  @Parameter(property = "closureExterns")
  private ArrayList<String> closureExterns;

  /**
   * Collects information mapping the generated (compiled) source back to its original source for debugging
   * purposes.<br/>
   * Please visit <a href="https://docs.google.com/document/d/1U1RGAehQwRypUTovF1KRlpiOFze0b-_2gc6fAH0KY0k/edit">Source
   * Map Revision 3 Proposal</a> for more information.
   * @since 1.7.3
   */
  @Parameter(property = "closureCreateSourceMap", defaultValue = "false")
  private boolean closureCreateSourceMap;

  /**
   * If true, the source map created by the Closure compiler will have one link to each of the original JavaScript
   * source files.
   * @since 1.7.5
   */
  @Parameter(property = "closureMapToOriginalSourceFiles", defaultValue = "false")
  private boolean closureMapToOriginalSourceFiles;

  /**
   * If {@code true}, include the content of the source file in the source map directly (via the {@code sourceContent}
   * property). This makes the source file bigger, but does not require the original source file to be added to the
   * browser dev tools.
   * @since 2.0.0
   */
  @Parameter(property = "closureIncludeSourcesContent", defaultValue = "false")
  private boolean closureIncludeSourcesContent;

  /**
   * <p>
   * Enables or disables sorting mode for Closure Library dependencies.
   * </p>
   * <p>
   * If true, automatically sort dependencies so that a file that {@code goog.provides} symbol X will always come
   * @since 1.7.4
   */
  @Parameter(property = "closureSortDependencies", defaultValue = "false")
  private boolean closureSortDependencies;

  /**
   * Treat certain warnings as the specified CheckLevel:
   * <ul>
   * <li>{@code ERROR}: Makes all warnings of the given group to build-breaking error.</li>
   * <li>{@code WARNING}: Makes all warnings of the given group a non-breaking warning.</li>
   * <li>{@code OFF}: Silences all warnings of the given group.</li>
   * </ul>
   * Example:
   * 
   * <pre>
   * <code class="language-java">
   * &lt;closureWarningLevels>
   *     &lt;nonStandardJsDocs>OFF&lt;/nonStandardJsDocs>
   * &lt;/closureWarningLevels>
   * </code>
   * </pre>
   * 
   * For the complete list of diagnostic groups please visit <a href=
   * "https://github.com/google/closure-compiler/wiki/Warnings">https://github.com/google/closure-compiler/wiki/Warnings</a>.
   * @since 1.7.5
   */
  @Parameter(property = "closureWarningLevels")
  private HashMap<String, String> closureWarningLevels;

  /**
   * Generate {@code $inject} properties for AngularJS for functions annotated with {@code @ngInject}.
   * @since 1.7.3
   */
  @Parameter(property = "closureAngularPass", defaultValue = "false")
  private boolean closureAngularPass;

  /**
   * A whitelist of tag names in JSDoc. Needed to support JSDoc extensions like ngdoc.
   * @since 1.7.5
   */
  @Parameter(property = "closureExtraAnnotations")
  private ArrayList<String> closureExtraAnnotations;

  /**
   * Override the value of variables annotated with {@code @define}.<br/>
   * The format is:
   * 
   * <pre>
   * <code class="language-java">
   * &lt;define>
   *     &lt;name>value&lt;/name>
   * &lt;/define>
   * </code>
   * </pre>
   * 
   * where {@code <name>} is the name of a {@code @define} variable and {@code value} is a boolean, number or string.
   * @since 1.7.5
   */
  @Parameter(property = "closureDefine")
  private HashMap<String, String> closureDefine;

  /**
   * Executed when the goal is invoked, it will first invoke a parallel lifecycle, ending at the given phase.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipMerge && skipMinify) {
      getLog().warn("Both merge and minify steps are configured to be skipped.");
      return;
    }

    fillOptionalValues();

    ClosureConfig closureConfig = fillClosureConfig();
    Collection<ProcessFilesTask> processFilesTasks;
    try {
      processFilesTasks = createTasks(closureConfig);
    }
    catch (FileNotFoundException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }

    ExecutorService executor = Executors.newFixedThreadPool(processFilesTasks.size());
    try {
      List<Future<Object>> futures = executor.invokeAll(processFilesTasks);
      for (Future<Object> future : futures) {
        try {
          future.get();
        }
        catch (ExecutionException e) {
          throw new MojoExecutionException(e.getMessage(), e);
        }
      }
      executor.shutdown();
    }
    catch (InterruptedException e) {
      executor.shutdownNow();
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void fillOptionalValues() {
    if (Strings.isNullOrEmpty(jsTargetDir)) {
      jsTargetDir = jsSourceDir;
    }
    if (Strings.isNullOrEmpty(charset)) {
      charset = Charset.defaultCharset().name();
    }
  }

  private ClosureConfig fillClosureConfig() throws MojoFailureException {
    DependencyOptions dependencyOptions = new DependencyOptions();
    dependencyOptions.setDependencySorting(closureSortDependencies);

    List<SourceFile> externs = new ArrayList<>();
    for (String extern : closureExterns) {
      externs.add(SourceFile.fromFile(webappSourceDir + File.separator + extern, Charset.forName(charset)));
    }

    Map<DiagnosticGroup, CheckLevel> warningLevels = new HashMap<>();
    DiagnosticGroups diagnosticGroups = new DiagnosticGroups();
    for (Map.Entry<String, String> warningLevel : closureWarningLevels.entrySet()) {
      DiagnosticGroup diagnosticGroup = diagnosticGroups.forName(warningLevel.getKey());
      if (diagnosticGroup == null) { throw new MojoFailureException("Failed to process closureWarningLevels: " + warningLevel.getKey() + " is an invalid DiagnosticGroup"); }

      try {
        CheckLevel checkLevel = CheckLevel.valueOf(warningLevel.getValue());
        warningLevels.put(diagnosticGroup, checkLevel);
      }
      catch (IllegalArgumentException e) {
        throw new MojoFailureException("Failed to process closureWarningLevels: " + warningLevel.getKey() + " is an invalid CheckLevel");
      }
    }

    return new ClosureConfig(closureLanguageIn, closureLanguageOut, closureEnvironment, closureCompilationLevel,
        dependencyOptions, externs, closureCreateSourceMap, warningLevels, closureAngularPass,
        closureExtraAnnotations, closureDefine, closureMapToOriginalSourceFiles, closureIncludeSourcesContent);
  }

  private Collection<ProcessFilesTask> createTasks(ClosureConfig closureConfig)
      throws MojoFailureException, FileNotFoundException {
    List<ProcessFilesTask> tasks = newArrayList();

    if (!Strings.isNullOrEmpty(bundleConfiguration)) { // If a bundleConfiguration is defined, attempt to use that
      AggregationConfiguration aggregationConfiguration;
      try (Reader bundleConfigurationReader = new FileReader(bundleConfiguration)) {
        aggregationConfiguration = new Gson().fromJson(bundleConfigurationReader,
            AggregationConfiguration.class);
      }
      catch (IOException e) {
        throw new MojoFailureException("Failed to open the bundle configuration file [" + bundleConfiguration
            + "].", e);
      }

      for (Aggregation aggregation : aggregationConfiguration.getBundles()) {
        if (Aggregation.AggregationType.js.equals(aggregation.getType())) {
          tasks.add(createJSTask(closureConfig, aggregation.getFiles(),
              Collections.<String> emptyList(), Collections.<String> emptyList(), aggregation.getName()));
        }
      }
    }
    else {
      // Otherwise, fallback to the default behavior
      tasks.add(createJSTask(closureConfig, jsSourceFiles, jsSourceIncludes, jsSourceExcludes,
          jsFinalFile));
    }

    return tasks;
  }

  private ProcessFilesTask createJSTask(ClosureConfig closureConfig, List<String> jsSourceFiles,
      List<String> jsSourceIncludes, List<String> jsSourceExcludes, String jsFinalFile)
      throws FileNotFoundException {
    return new ProcessJSFilesTask(getLog(), verbose, bufferSize, Charset.forName(charset), suffix, nosuffix,
        skipMerge, skipMinify, webappSourceDir, webappTargetDir, jsSourceDir, jsSourceFiles, jsSourceIncludes,
        jsSourceExcludes, jsTargetDir, jsFinalFile, closureConfig);
  }
}
