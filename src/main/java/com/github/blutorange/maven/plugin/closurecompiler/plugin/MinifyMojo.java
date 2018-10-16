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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.blutorange.maven.plugin.closurecompiler.common.Aggregation;
import com.github.blutorange.maven.plugin.closurecompiler.common.AggregationConfiguration;
import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileProcessConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSpecifier;
import com.github.blutorange.maven.plugin.closurecompiler.common.LogLevel;
import com.github.blutorange.maven.plugin.closurecompiler.common.LogWrapper;
import com.github.blutorange.maven.plugin.closurecompiler.common.MojoMetaImpl;
import com.github.blutorange.maven.plugin.closurecompiler.common.SourceMapOutputType;
import com.google.gson.Gson;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.DependencyMode;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

/**
 * Goal for combining and/or minifying JavaScript files with closure compiler.
 */
@Mojo(name = "minify", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class MinifyMojo extends AbstractMojo {

  /* ************* */
  /* Maven options */
  /* ************* */

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  private Log logWrapper;

  /* ************** */
  /* Global Options */
  /* ************** */

  /**
   * By default, messages are logged at the log level set by maven. This option allows you to change the log level.
   * Valid options are {@code all}, {@code debug}, {@code info}, {@code warn}, {@code error}, {@code none}. Leave empty
   * to use the default log level. Please note that you can only decrease, not increase, the log level.
   */
  @Parameter(property = "logLevel", defaultValue = "")
  private LogLevel logLevel;

  /**
   * For each bundle, this plugin performs a check whether the input or output files have changed and skips the
   * execution in case they haven't. Set this flag to {@code true} to force the execution.
   * @since 2.0.0
   */
  @Parameter(property = "force", defaultValue = "false")
  private boolean force;

  /**
   * Size of the buffer used to read source files.
   */
  @Parameter(property = "bufferSize", defaultValue = "4096")
  private int bufferSize;

  /**
   * The line separator to be used when merging files etc. Defaults to the default system line separator. Special
   * characters are entered escaped. So for example, to use a new line feed as the separator, set this property to
   * {@code \n} (two characters, a backslash and the letter n).
   * @since 2.0.0
   */
  @Parameter(property = "lineSeparator", defaultValue = "")
  private String lineSeparator;

  /**
   * If a supported character set is specified, it will be used to read the input file. Otherwise, it will assume that
   * the platform's default character set is being used. The output file is encoded using the same character set.<br/>
   * See the <a href="http://www.iana.org/assignments/character-sets">IANA Charset Registry</a> for a list of valid
   * encoding types.
   * @since 1.3.2
   */
  @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}", alias = "charset")
  private String encoding;

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
   * Specify aggregations in an external JSON formatted config file. If not an absolute path, it must be relative to the
   * project base directory.
   * @since 1.7.5
   */
  @Parameter(property = "bundleConfiguration", defaultValue = "")
  private String bundleConfiguration;

  /* *********** */
  /* File Options */
  /* ************ */

  /**
   * <p>
   * The output file name of the processed files.
   * </p>
   * <p>
   * Variables are specified via <code>#{variableName}</code>. To insert a literal {@code #}, use {@code ##}. The
   * variable {@code filename} is replaced with the name of the minified file; the variable {@code extension} with the
   * extension of the file; and the variable {@code basename} with the basename (name without the extension) of the
   * file.
   * </p>
   * <p>
   * If merging files, by default the basename is set to {@code script} and the extension to {@code js}, so that the
   * resulting merged file is called {@code script.min.js}.
   * </p>
   * @since 2.0.0
   */
  @Parameter(property = "outputFileName", defaultValue = "#{basename}.min.#{extension}")
  private String outputFilename;

  /**
   * Base directory for source files. This should be an absolute path; if not, it must be relative to the project base
   * directory. Use variables such as {@code basedir} to make it relative to the current directory.
   */
  @Parameter(property = "baseSourceDir", defaultValue = "${basedir}/src/main/webapp")
  private File baseSourceDir;

  /**
   * Base directory for output files. This should be an absolute path; if not, it must be relative to the project base
   * directory. Use variables such as {@code project.build.directory} to make it relative to the current directory.
   */
  @Parameter(property = "baseTargetDir", defaultValue = "${project.build.directory}/${project.build.finalName}")
  private File baseTargetDir;

  /**
   * JavaScript source directory. This is relative to the {@link #baseSourceDir}.
   */
  @Parameter(property = "sourceDir", defaultValue = "js")
  private String sourceDir;

  /**
   * JavaScript files to include. Specified as fileset patterns which are relative to the JavaScript source directory.
   * @since 1.2
   */
  @Parameter(property = "includes")
  private ArrayList<String> includes;

  /**
   * JavaScript files to exclude. Specified as fileset patterns which are relative to the JavaScript source directory.
   * @since 1.2
   */
  @Parameter(property = "excludes")
  private ArrayList<String> excludes;

  /**
   * JavaScript target directory. Takes the same value as {@code jsSourceDir} when empty. This is relative to the
   * {@link #baseTargetDir}.
   * @since 1.3.2
   */
  @Parameter(property = "targetDir", defaultValue = "js")
  private String targetDir;

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
   * <li>{@code STABLE} Use stable features
   * </ul>
   * @since 1.7.2
   */
  @Parameter(property = "closureLanguageIn", defaultValue = "ECMASCRIPT_2016")
  private LanguageMode closureLanguageIn;

  /**
   * Refers to which version of ECMAScript your code will be returned in.<br/>
   * It is used to transpile between different levels of ECMAScript. Possible values are
   * <ul>
   * <li>{@code ECMASCRIPT3}
   * <li>{@code ECMASCRIPT5}
   * <li>{@code ECMASCRIPT5_STRICT}
   * <li>{@code ECMASCRIPT_2015}
   * <li>{@code STABLE}
   * <li>{@code NO_TRANSPILE}</li>
   * </ul>
   * @since 1.7.5
   */
  @Parameter(property = "closureLanguageOut", defaultValue = "ECMASCRIPT_2015")
  private LanguageMode closureLanguageOut;

  /**
   * Extension of the source map, if one is created. By default, the extension {@code .map} is added to the minified
   * file. Variables are specified via <code>#{variableName}</code>. To insert a literal {@code #}, use {@code ##}. The
   * variable {@code filename} is replaced with the name of the minified file; the variable {@code extension} with the
   * extension of the file; and the variable {@code basename} with the basename (name without the extension) of the
   * file.
   * @since 2.0.0
   */
  @Parameter(property = "closureSourceMapName", defaultValue = "#{filename}.map")
  private String closureSourceMapName;

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
   * <li>{@code BUNDLE}: Leaves all compiler options unchanged. For advanced usage if you want to seet the releavant
   * options yourself.</li>
   * </ul>
   * @since 1.7.2
   */
  @Parameter(property = "closureCompilationLevel", defaultValue = "SIMPLE_OPTIMIZATIONS")
  private CompilationLevel closureCompilationLevel;

  /**
   * List of JavaScript files containing code that declares function names or other symbols. Use {@code closureExterns}
   * to preserve symbols that are defined outside of the code you are compiling. The {@code closureExterns} parameter
   * only has an effect if you are using a {@code CompilationLevel} of {@code ADVANCED_OPTIMIZATIONS}.<br/>
   * These file names are relative to {@link #baseSourceDir} directory.
   * @since 1.7.2
   */
  @Parameter(property = "closureExterns")
  private ArrayList<String> closureExterns;

  /**
   * <p>
   * Collects information mapping the generated (compiled) source back to its original source for debugging purposes.
   * </p>
   * @since 1.7.3
   */
  @Parameter(property = "closureCreateSourceMap", defaultValue = "false")
  private boolean closureCreateSourceMap;

  /**
   * Whether the error output from the closure compiler is colorized. Color codes may not be supported by all terminals.
   * @since 2.0.0
   */
  @Parameter(property = "closureColorizeErrorOutput", defaultValue = "true")
  private boolean closureColorizeErrorOutput;

  /**
   * If {@code true}, the processed ("minified") file is pretty printed (formatted with new lines).
   * @since 2.0.0
   */
  @Parameter(property = "closurePrettyPrint", defaultValue = "false")
  private boolean closurePrettyPrint;

  /**
   * If {@code true}, ES6 polyfills are written to the output file (such as for Set, Map etc.)
   * @since 2.0.0
   */
  @Parameter(property = "closureRewritePolyfills", defaultValue = "true")
  private boolean closureRewritePolyfills;

  /**
   * If {@code false}, converts some characters such as '&lt;' and '&gt;' to '\x3c' and '\x3d' so that they are safe to
   * put inside a script tag in an HTML file.
   * @since 2.0.0
   */
  @Parameter(property = "closureTrustedStrings", defaultValue = "true")
  private boolean closureTrustedStrings;

  /**
   * <p>
   * If not an empty or blank string, interpolate output into this string at the place denoted by the marker token
   * {@code %output%}. Use marker token {@code %output|jsstring%} to do js string escaping on the output.
   * </p>
   * <p>
   * When using this options with a source map, the map is adjusted appropriately to match the code.
   * </p>
   * @since 2.0.0
   */
  @Parameter(property = "closureOutputWrapper", defaultValue = "")
  private String closureOutputWrapper;

  /**
   * If {@code true}, include the content of the source file in the source map directly (via the {@code sourceContent}
   * property). This makes the source file bigger, but does not require the original source file to be added to the
   * browser dev tools.
   * @since 2.0.0
   */
  @Parameter(property = "closureIncludeSourcesContent", defaultValue = "false")
  private boolean closureIncludeSourcesContent;

  /**
   * How compiler should prune files based on the provide-require dependency graph.
   * <ul>
   * <li>{@code NONE} All files will be included in the compilation</li>
   * <li>{@code LOOSE}Files must be discoverable from specified entry points. Files which do not goog.provide a
   * namespace and are not either an ES6 or CommonJS module will be automatically treated as entry points. Module files
   * will be included only if referenced from an entry point.</li>
   * <li>{@code STRICT}Files must be discoverable from specified entry points. Files which do not goog.provide a
   * namespace and are neither an ES6 or CommonJS module will be dropped. Module files will be included only if
   * referenced from an entry point.</li>
   * </ul>
   * @since 2.0.0
   */
  @Parameter(property = "closureDependencyMode", defaultValue = "NONE")
  private DependencyMode closureDependencyMode;

  /**
   * <p>
   * When you use {@code closureDependencyMode} STRICT or LOOSE, you must specify to the compiler what the entry points
   * of your application are. Beginning at those entry points, it will trace through the files to discover what sources
   * are actually referenced and will drop all other files.
   * </p>
   * <p>
   * Adds a collection of symbols to always keep. In dependency pruning mode, we will automatically keep all the
   * transitive dependencies of these symbols. The syntactic form of a symbol depends on the type of dependency
   * primitives we're using. For example, {@code goog.provide('foo.bar')} provides the symbol {@code foo.bar}. Entry
   * points can be scoped to a module by specifying {@code mod2:foo.bar}.
   * </p>
   * <p>
   * There are two different types of entry points, closures and modules:
   * <ul>
   * <li>{@code closure}: A closure namespace used as an entry point. May start with {@code goog:} when provided as a
   * flag from the command line. Closure entry points may also be formatted as: {@code goog:moduleName:name.space} which
   * specifies that the module name and provided namespace are different</li>
   * <li>{@code file}: Must start with the prefix {@code file:}. AES6 or CommonJS modules used as an entry point. The
   * file path is relative to the {@code sourceDir}.</li>
   * </ul>
   * @since 2.0.0
   */
  @Parameter(property = "closureDependencyEntryPoints")
  private List<String> closureDependencyEntryPoints;

  /**
   * After creating the source map, the browser needs to find it. There are several options available:
   * <ul>
   * <li>{@code reference} (the default): Create a source map named [originalFile].map, and add a reference to it in the
   * minified file.</li>
   * <li>{@code file}: Just create a source map named [originalFile].map, do not add a reference in the minified file.
   * This may be useful when you want to add the {@code Source-Map} HTTP header.</li>
   * <li>{@code file}: Do not write a separate source map file, but instead include the source file content in the
   * minified file (as base64). This makes it easier for the browser to find the source map. Especially useful when used
   * with JSF/Primefaces.</li>
   * </ul>
   * @since 2.0.0
   */
  @Parameter(property = "closureSourceMapOutputType", defaultValue = "reference")
  private SourceMapOutputType closureSourceMapOutputType;

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
   * where {@code <name>} is the name of a {@code @define} variable and {@code value} is a JavaScript boolean, number or
   * string literal. That is, use quotation marks to specify a string: {@code "First line\nseconds line"}
   * @since 1.7.5
   */
  @Parameter(property = "closureDefineReplacements")
  private HashMap<String, String> closureDefineReplacements;

  private ProcessFilesTask createJSTask(ClosureConfig closureConfig,
      List<String> includes, List<String> excludes, String outputFilename)
      throws IOException {
    FileProcessConfig processConfig = new FileProcessConfig(lineSeparator, bufferSize, force, skipMerge, skipMinify);
    FileSpecifier fileSpecifier = new FileSpecifier(baseSourceDir, baseTargetDir, sourceDir, targetDir, includes, excludes, outputFilename);
    MojoMetadata mojoMeta = new MojoMetaImpl(project, getLog(), encoding, buildContext);
    return new ProcessJSFilesTask(mojoMeta, processConfig, fileSpecifier, closureConfig);
  }

  private Collection<ProcessFilesTask> createTasks(ClosureConfig closureConfig)
      throws MojoFailureException, IOException {
    List<ProcessFilesTask> tasks = newArrayList();

    // If a bundleConfiguration is defined, attempt to use that
    if (StringUtils.isNotBlank(bundleConfiguration)) {
      for (Aggregation aggregation : getAggregations()) {
        tasks.add(createJSTask(closureConfig, aggregation.getIncludes(),
            aggregation.getExcludes(), aggregation.getName()));
      }
    }
    // Otherwise, fallback to the default behavior
    else {
      tasks.add(createJSTask(closureConfig, includes, excludes, outputFilename));
    }

    return tasks;
  }

  /**
   * Executed when the goal is invoked, it will first invoke a parallel lifecycle, ending at the given phase.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipMerge && skipMinify) {
      getLog().warn("Both merge and minify steps are configured to be skipped. Files will only be copied to their destination without any processing");
    }

    fillOptionalValues();

    ClosureConfig closureConfig = new ClosureConfig(this);
    Collection<ProcessFilesTask> processFilesTasks;
    try {
      processFilesTasks = createTasks(closureConfig);
    }
    catch (IOException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }

    // ExecutorService executor = Executors.newFixedThreadPool(processFilesTasks.size());
    ExecutorService executor = Executors.newSingleThreadExecutor();
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
    if (StringUtils.isBlank(targetDir)) {
      targetDir = sourceDir;
    }
    if (StringUtils.isBlank(encoding)) {
      encoding = Charset.defaultCharset().name();
    }
    if (StringUtils.isBlank(lineSeparator)) {
      lineSeparator = System.lineSeparator();
    }
    else {
      lineSeparator = StringEscapeUtils.unescapeJava(lineSeparator);
    }
    if (excludes == null) {
      excludes = new ArrayList<>();
    }
    if (includes == null) {
      includes = new ArrayList<>();
    }
    if (closureWarningLevels == null) {
      closureWarningLevels = new HashMap<>();
    }
    if (closureExtraAnnotations == null) {
      closureExtraAnnotations = new ArrayList<>();
    }
    if (closureDefineReplacements == null) {
      closureDefineReplacements = new HashMap<>();
    }
    if (closureExterns == null) {
      closureExterns = new ArrayList<>();
    }
    if (closureDependencyEntryPoints == null) {
      closureDependencyEntryPoints = new ArrayList<>();
    }
  }

  private Collection<Aggregation> getAggregations() throws MojoFailureException {
    if (StringUtils.isBlank(bundleConfiguration)) { return Collections.emptySet(); }
    AggregationConfiguration aggregationConfiguration;
    try (Reader bundleConfigurationReader = new FileReader(FileHelper.getAbsoluteFile(project.getBasedir(), bundleConfiguration))) {
      aggregationConfiguration = new Gson().fromJson(bundleConfigurationReader,
          AggregationConfiguration.class);
    }
    catch (IOException e) {
      throw new MojoFailureException("Failed to open the bundle configuration file [" + bundleConfiguration
          + "].", e);
    }
    return CollectionUtils.emptyIfNull(aggregationConfiguration.getBundles());
  }

  public File getBaseSourceDir() {
    return baseSourceDir;
  }

  public File getBaseTargetDir() {
    return baseTargetDir;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public BuildContext getBuildContext() {
    return buildContext;
  }

  public String getBundleConfiguration() {
    return bundleConfiguration;
  }

  public CompilationLevel getClosureCompilationLevel() {
    return closureCompilationLevel;
  }

  public HashMap<String, String> getClosureDefineReplacements() {
    return closureDefineReplacements;
  }

  public List<String> getClosureDependencyEntryPoints() {
    return closureDependencyEntryPoints;
  }

  public CompilerOptions.Environment getClosureEnvironment() {
    return closureEnvironment;
  }

  public ArrayList<String> getClosureExterns() {
    return closureExterns;
  }

  public ArrayList<String> getClosureExtraAnnotations() {
    return closureExtraAnnotations;
  }

  public LanguageMode getClosureLanguageIn() {
    return closureLanguageIn;
  }

  public LanguageMode getClosureLanguageOut() {
    return closureLanguageOut;
  }

  public String getClosureOutputWrapper() {
    return closureOutputWrapper;
  }

  public String getClosureSourceMapName() {
    return closureSourceMapName;
  }

  public SourceMapOutputType getClosureSourceMapOutputType() {
    return closureSourceMapOutputType;
  }

  public HashMap<String, String> getClosureWarningLevels() {
    return closureWarningLevels;
  }

  public String getEncoding() {
    return encoding;
  }

  public ArrayList<String> getExcludes() {
    return excludes;
  }

  public ArrayList<String> getIncludes() {
    return includes;
  }

  public String getLineSeparator() {
    return lineSeparator;
  }

  @Override
  public Log getLog() {
    if (logWrapper == null) {
      logWrapper = new LogWrapper(super.getLog(), logLevel);
    }
    return logWrapper;
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }

  public Log getLogWrapper() {
    return logWrapper;
  }

  public String getOutputFilename() {
    return outputFilename;
  }

  public MavenProject getProject() {
    return project;
  }

  public String getSourceDir() {
    return sourceDir;
  }

  public String getTargetDir() {
    return targetDir;
  }

  public boolean isClosureAngularPass() {
    return closureAngularPass;
  }

  public boolean isClosureColorizeErrorOutput() {
    return closureColorizeErrorOutput;
  }

  public boolean isClosureCreateSourceMap() {
    return closureCreateSourceMap;
  }

  public boolean isClosureIncludeSourcesContent() {
    return closureIncludeSourcesContent;
  }

  public boolean isClosurePrettyPrint() {
    return closurePrettyPrint;
  }

  public boolean isClosureRewritePolyfills() {
    return closureRewritePolyfills;
  }

  public boolean isClosureTrustedStrings() {
    return closureTrustedStrings;
  }

  public boolean isForce() {
    return force;
  }

  public boolean isSkipMerge() {
    return skipMerge;
  }

  public boolean isSkipMinify() {
    return skipMinify;
  }

  public void setBaseSourceDir(File baseSourceDir) {
    this.baseSourceDir = baseSourceDir;
  }

  public void setBaseTargetDir(File baseTargetDir) {
    this.baseTargetDir = baseTargetDir;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public void setBuildContext(BuildContext buildContext) {
    this.buildContext = buildContext;
  }

  public void setBundleConfiguration(String bundleConfiguration) {
    this.bundleConfiguration = bundleConfiguration;
  }

  public void setClosureAngularPass(boolean closureAngularPass) {
    this.closureAngularPass = closureAngularPass;
  }

  public void setClosureColorizeErrorOutput(boolean closureColorizeErrorOutput) {
    this.closureColorizeErrorOutput = closureColorizeErrorOutput;
  }

  public void setClosureCompilationLevel(CompilationLevel closureCompilationLevel) {
    this.closureCompilationLevel = closureCompilationLevel;
  }

  public void setClosureCreateSourceMap(boolean closureCreateSourceMap) {
    this.closureCreateSourceMap = closureCreateSourceMap;
  }

  public void setClosureDefineReplacements(HashMap<String, String> closureDefineReplacements) {
    this.closureDefineReplacements = closureDefineReplacements;
  }

  public void setClosureDependencyEntryPoints(List<String> closureDependencyEntryPoints) {
    this.closureDependencyEntryPoints = closureDependencyEntryPoints;
  }

  public void setClosureEnvironment(CompilerOptions.Environment closureEnvironment) {
    this.closureEnvironment = closureEnvironment;
  }

  public void setClosureExterns(ArrayList<String> closureExterns) {
    this.closureExterns = closureExterns;
  }

  public void setClosureExtraAnnotations(ArrayList<String> closureExtraAnnotations) {
    this.closureExtraAnnotations = closureExtraAnnotations;
  }

  public void setClosureIncludeSourcesContent(boolean closureIncludeSourcesContent) {
    this.closureIncludeSourcesContent = closureIncludeSourcesContent;
  }

  public void setClosureLanguageIn(LanguageMode closureLanguageIn) {
    this.closureLanguageIn = closureLanguageIn;
  }

  public void setClosureLanguageOut(LanguageMode closureLanguageOut) {
    this.closureLanguageOut = closureLanguageOut;
  }

  public void setClosureOutputWrapper(String closureOutputWrapper) {
    this.closureOutputWrapper = closureOutputWrapper;
  }

  public void setClosurePrettyPrint(boolean closurePrettyPrint) {
    this.closurePrettyPrint = closurePrettyPrint;
  }

  public void setClosureRewritePolyfills(boolean closureRewritePolyfills) {
    this.closureRewritePolyfills = closureRewritePolyfills;
  }

  public void setClosureSourceMapName(String closureSourceMapName) {
    this.closureSourceMapName = closureSourceMapName;
  }

  public void setClosureSourceMapOutputType(SourceMapOutputType closureSourceMapOutputType) {
    this.closureSourceMapOutputType = closureSourceMapOutputType;
  }

  public void setClosureTrustedStrings(boolean closureTrustedStrings) {
    this.closureTrustedStrings = closureTrustedStrings;
  }

  public void setClosureWarningLevels(HashMap<String, String> closureWarningLevels) {
    this.closureWarningLevels = closureWarningLevels;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setExcludes(ArrayList<String> excludes) {
    this.excludes = excludes;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public void setIncludes(ArrayList<String> includes) {
    this.includes = includes;
  }

  public void setLineSeparator(String lineSeparator) {
    this.lineSeparator = lineSeparator;
  }

  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  public void setLogWrapper(Log logWrapper) {
    this.logWrapper = logWrapper;
  }

  public DependencyMode getClosureDependencyMode() {
    return closureDependencyMode;
  }

  public void setClosureDependencyMode(DependencyMode closureDependencyMode) {
    this.closureDependencyMode = closureDependencyMode;
  }

  public void setOutputFilename(String outputFilename) {
    this.outputFilename = outputFilename;
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public void setSkipMerge(boolean skipMerge) {
    this.skipMerge = skipMerge;
  }

  public void setSkipMinify(boolean skipMinify) {
    this.skipMinify = skipMinify;
  }

  public void setSourceDir(String sourceDir) {
    this.sourceDir = sourceDir;
  }

  public void setTargetDir(String targetDir) {
    this.targetDir = targetDir;
  }
}
