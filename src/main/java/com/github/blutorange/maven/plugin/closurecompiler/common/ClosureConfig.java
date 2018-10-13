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
package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.DependencyOptions;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;
import com.google.javascript.jscomp.SourceMap.Format;

/**
 * <a href="https://developers.google.com/closure/compiler/">Google Closure Compiler</a> configuration.
 */
public class ClosureConfig {

  private final LanguageMode languageIn;

  private final LanguageMode languageOut;

  private final CompilerOptions.Environment environment;

  private final CompilationLevel compilationLevel;

  private final DependencyOptions dependencyOptions;

  private final List<SourceFile> externs;

  private final Format sourceMapFormat;

  private final Map<DiagnosticGroup, CheckLevel> warningLevels;

  private final boolean colorizeErrorOutput;

  private final boolean angularPass;

  private final List<String> extraAnnotations;

  private final Map<String, Object> defineReplacements = new HashMap<>();

  private final boolean mapToOriginalSourceFiles;

  private final boolean includeSourcesContent;

  private final SourceMapOutputType sourceMapOutputType;

  private boolean prettyPrint;

  private boolean rewritePolyfills;

  private boolean trustedStrings;

  private String outputWrapper;

  /**
   * Init Closure Compiler values.
   * @param languageIn the version of ECMAScript used to report errors in the code
   * @param languageOut the version of ECMAScript the code will be returned in
   * @param environment the set of builtin externs to load
   * @param compilationLevel the degree of compression and optimization to apply to JavaScript
   * @param dependencyOptions options for how to manage dependencies between input files
   * @param externs preserve symbols that are defined outside of the code you are compiling
   * @param createSourceMap create a source map for the minifed/combined production files
   * @param warningLevels a map of warnings to enable or disable in the compiler
   * @param angularPass use {@code @ngInject} annotation to generate Angular injections
   * @param extraAnnotations make extra annotations known to the closure engine
   * @param defineReplacements replacements for {@code @defines}
   * @param mapToOriginalSourceFiles if true, do not merge the source js files and create a link to each of them in the
   * @param closureSourceMapInclusionType
   * @param includeSourceContent If true, include the content of the source file in the source map. source map
   * @param sourceMapOutputType How to include the source map (inline or separate file).
   * @param prettyPrint
   * @param rewritePolyfills
   * @param outputWrapper
   */
  public ClosureConfig(LanguageMode languageIn, LanguageMode languageOut, CompilerOptions.Environment environment,
      CompilationLevel compilationLevel, DependencyOptions dependencyOptions,
      List<SourceFile> externs, boolean createSourceMap,
      Map<DiagnosticGroup, CheckLevel> warningLevels, boolean angularPass,
      List<String> extraAnnotations, Map<String, String> defineReplacements, boolean mapToOriginalSourceFiles, boolean includeSourcesContent,
      SourceMapOutputType sourceMapOutputType, boolean prettyPrint, boolean rewritePolyfills, boolean trustedStrings,
      String outputWrapper) {
    this.languageIn = languageIn;
    this.languageOut = languageOut;
    this.environment = environment;
    this.mapToOriginalSourceFiles = createSourceMap && mapToOriginalSourceFiles;
    this.compilationLevel = compilationLevel;
    this.dependencyOptions = dependencyOptions;
    this.externs = externs;
    this.sourceMapFormat = (createSourceMap) ? SourceMap.Format.V3 : null;
    this.warningLevels = warningLevels;
    this.colorizeErrorOutput = Boolean.TRUE;
    this.angularPass = angularPass;
    this.extraAnnotations = extraAnnotations;
    this.includeSourcesContent = includeSourcesContent;
    this.sourceMapOutputType = sourceMapOutputType;
    this.prettyPrint = prettyPrint;
    this.rewritePolyfills = rewritePolyfills;
    this.trustedStrings = trustedStrings;
    this.outputWrapper = outputWrapper;

    for (Map.Entry<String, String> defineReplacement : defineReplacements.entrySet()) {
      if (Strings.isNullOrEmpty(defineReplacement.getValue())) { throw new RuntimeException("Define replacement " + defineReplacement.getKey() + " does not have a value."); }

      if (String.valueOf(true).equals(defineReplacement.getValue()) ||
          String.valueOf(false).equals(defineReplacement.getValue())) {
        this.defineReplacements.put(defineReplacement.getKey(), Boolean.valueOf(defineReplacement.getValue()));
        continue;
      }

      try {
        this.defineReplacements.put(defineReplacement.getKey(), Integer.valueOf(defineReplacement.getValue()));
        continue;
      }
      catch (NumberFormatException e) {
        // Not a valid Integer, try next type
      }

      try {
        this.defineReplacements.put(defineReplacement.getKey(), Double.valueOf(defineReplacement.getValue()));
        continue;
      }
      catch (NumberFormatException e) {
        // Not a valid Double, try next type
      }

      this.defineReplacements.put(defineReplacement.getKey(), defineReplacement.getValue());
    }
  }

  public LanguageMode getLanguageIn() {
    return languageIn;
  }

  public LanguageMode getLanguageOut() {
    return languageOut;
  }

  public CompilerOptions.Environment getEnvironment() {
    return environment;
  }

  public CompilationLevel getCompilationLevel() {
    return compilationLevel;
  }

  public DependencyOptions getDependencyOptions() {
    return dependencyOptions;
  }

  public List<SourceFile> getExterns() {
    return externs;
  }

  public Format getSourceMapFormat() {
    return sourceMapFormat;
  }

  public Map<DiagnosticGroup, CheckLevel> getWarningLevels() {
    return warningLevels;
  }

  public Boolean getColorizeErrorOutput() {
    return colorizeErrorOutput;
  }

  public Boolean getAngularPass() {
    return angularPass;
  }

  public List<String> getExtraAnnotations() {
    return extraAnnotations;
  }

  public Map<String, Object> getDefineReplacements() {
    return defineReplacements;
  }

  public Boolean getMapToOriginalSourceFiles() {
    return mapToOriginalSourceFiles;
  }

  public boolean getIncludeSourcesContent() {
    return includeSourcesContent;
  }

  public SourceMapOutputType getSourceMapOutputType() {
    return sourceMapOutputType;
  }

  public boolean getPrettyPrint() {
    return prettyPrint;
  }

  public boolean getRewritePolyfills() {
    return rewritePolyfills;
  }

  public boolean getTrustedStrings() {
    return trustedStrings;
  }

  public String getOutputWrapper() {
    return outputWrapper;
  }
}
