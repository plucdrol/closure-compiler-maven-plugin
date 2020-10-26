package com.github.blutorange.maven.plugin.closurecompiler.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MinifyMojoTest {

  private Logger LOG = Logger.getLogger(MinifyMojoTest.class.getCanonicalName());

  @Rule
  public TestResources testResources = new TestResources("src/test/resources/projects", "target/test-projects");

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testMinimal() throws Exception {
    runMinifyAndAssertDirContent("minimal");
  }

  @Test
  public void testCompilationLevel() throws Exception {
    runMinifyAndAssertDirContent("compilationlevel");
  }

  @Test
  public void testDefine() throws Exception {
    runMinifyAndAssertDirContent("define");
  }

  @Test
  public void testUseTypesForOptimization() throws Exception {
    runMinifyAndAssertDirContent("usetypesforoptimization");
  }

  @Test
  public void testExterns() throws Exception {
    // No externs declared, variable cannot be found, so minification should fail
    expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("externs", Arrays.asList("without-externs")));

    // Externs declared, variable can be found, so minification should succeed
    runMinifyAndAssertDirContent("externs", Arrays.asList("createOlderFile", "with-externs"));
  }

  @Test
  public void testOutputWrapper() throws Exception {
    runMinifyAndAssertDirContent("outputwrapper");
  }

  @Test
  public void testSkip() throws Exception {
    runMinifyAndAssertDirContent("skip");
  }

  @Test
  public void testSkipAll() throws Exception {
    runMinifyAndAssertDirContent("skipall");
  }

  @Test
  public void testNodeModules() throws Exception {
    runMinifyAndAssertDirContent("nodemodules");
  }

  @Test
  public void testOutputFilename() throws Exception {
    runMinifyAndAssertDirContent("outputfilename");
  }

  @Test
  public void testSourceMap() throws Exception {
    runMinifyAndAssertDirContent("sourcemap");
  }

  @Test
  public void testSubdirs() throws Exception {
    runMinifyAndAssertDirContent("subdirs");
  }

  @Test
  public void testSkipSome() throws Exception {
    runMinifyAndAssertDirContent("skipsome");
  }

  @Test
  public void testOverwriteInputFilesDisabled() throws Exception {
    MavenResult result = runMinify("overwriteInputFilesDisabled");
    assertTrue(result.getErrString().contains("The source file [fileC.js] has the same name as the output file [fileC.js]"));
  }

  @Test
  public void testOverwriteInputFilesEnabled() throws Exception {
    runMinifyAndAssertDirContent("overwriteInputFilesEnabled");
  }

  @Test
  public void testPreferSingleQuotes() throws Exception {
    runMinifyAndAssertDirContent("prefersinglequotes");
  }

  @Test
  public void testAssumeFunctionWrapper() throws Exception {
    runMinifyAndAssertDirContent("assumeFunctionWrapper");
  }

  @Test
  public void testSkipIfNewer() throws Exception {
    // Output file does not exists, minification should run
    expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("skipIfNewer")));

    // This create the newer output file, so the minification process should not run
    runMinifyAndAssertDirContent("skipif", Arrays.asList("createNewerFile", "skipIfNewer"));

    // Now force is enabled, minification should run
    expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("createNewerFile", "skipIfNewer", "force")));
  }

  @Test
  public void testSkipIfExists() throws Exception {
    // Output file does not exists, minification should run
    expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("skipIfExists")));

    // This create the (older) output file, so the minification process should not run
    runMinifyAndAssertDirContent("skipif", Arrays.asList("createOlderFile", "skipIfExists"));

    // Now force is enabled, minification should run
    expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("createOlderFile", "skipIfExists", "force")));
  }

  @Test
  public void testTrustedStrings() throws Exception {
    runMinifyAndAssertDirContent("trustedstrings");
  }

  @Test
  public void testPrettyPrint() throws Exception {
    runMinifyAndAssertDirContent("prettyprint");
  }

  @Test
  public void testEmitUseStrict() throws Exception {
    runMinifyAndAssertDirContent("emitusestrict");
  }

  @Test
  public void testRewritePolyfills() throws Exception {
    runMinifyAndAssertDirContent("rewritepolyfills");
  }

  @Test
  public void testJQuery() throws Exception {
    runMinifyAndAssertDirContent("jquery");
  }

  private <T extends Throwable> void expectError(Class<T> error, Action runnable) {
    try {
      runnable.run();
    }
    catch (Throwable e) {
      if (error.isInstance(e)) { return; }
      fail("Action threw an error of type " + e.getClass().getSimpleName() + ", but it is not of the expected type " + error.getSimpleName());
    }
    fail("Action did not throw the expected error type " + error.getSimpleName());
  }

  private void runMinifyAndAssertDirContent(String projectName) throws Exception {
    runMinifyAndAssertDirContent(projectName, new HashSet<>());
  }

  private MavenResult runMinify(String projectName) throws Exception {
    return runMinify(projectName, new HashSet<>());
  }

  private MavenResult runMinify(String projectName, Collection<String> profiles) throws Exception {
    File parentdir = testResources.getBasedir("parent").getCanonicalFile();
    File parentPom = new File(parentdir, "pom.xml");
    File parentPomNew = new File(parentdir.getParentFile(), "pom.xml");
    assertTrue(parentPom.exists());
    FileUtils.copyFile(parentPom, parentPomNew);

    File basedir = testResources.getBasedir(projectName).getCanonicalFile();
    File pom = new File(basedir, "pom.xml");
    assertTrue(pom.exists());

    clean(basedir);
    invokeMaven(parentPomNew, "install", Collections.emptySet());
    return invokeMaven(pom, "package", profiles);
  }

  private void runMinifyAndAssertDirContent(String projectName, Collection<String> profiles) throws Exception {
    File basedir = testResources.getBasedir(projectName).getCanonicalFile();
    runMinify(projectName, profiles);
    assertDirContent(basedir);
  }

  private MavenResult invokeMaven(File pom, String goal, Collection<String> profiles) throws IOException {
    MavenCli cli = new MavenCli();
    final List<String> args = new ArrayList<>();
    args.add("clean");
    args.add(goal);
    args.add("-DskipTests");
    profiles.stream().flatMap(profile -> Stream.of("-P", profile)).forEach(args::add);
    System.setProperty("maven.multiModuleProjectDirectory", pom.getParent());
    LOG.info("Invoking maven: " + StringUtils.join(args, " "));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream outStream = new ChainedPrintStream(new PrintStream(out), System.out);
    PrintStream errStream = new ChainedPrintStream(new PrintStream(err), System.err);
    cli.doMain(args.toArray(new String[0]), pom.getParent(), outStream, errStream);
    String outString = out.toString("utf-8");
    String errString = err.toString("utf-8");
    return new MavenResult(outString, errString);
  }

  private void assertDirContent(File basedir) {
    File expected = new File(basedir, "expected");
    File actual = new File(new File(basedir, "target"), "test");
    Map<String, File> expectedFiles = expected.exists() ? listFiles(expected) : new HashMap<>();
    Map<String, File> actualFiles = actual.exists() ? listFiles(actual) : new HashMap<>();
    LOG.info("Comparing actual files [\n" + actualFiles.values().stream().map(File::getAbsolutePath).collect(Collectors.joining(",\n")) + "\n]");
    LOG.info("to the expected files [\n" + expectedFiles.values().stream().map(File::getAbsolutePath).collect(Collectors.joining(",\n")) + "\n]");
    assertTrue("There must be at least one expected file. Add a file 'nofiles' if you expect there to be no files", expectedFiles.size() > 0);
    if (expectedFiles.size() == 1 && "nofiles".equals(expectedFiles.values().iterator().next().getName())) {
      // Expect there to be no output files
      assertEquals(0, actualFiles.size());
    }
    else {
      assertEquals("Number of expected files must match the number of produced files", expectedFiles.size(), actualFiles.size());
      assertTrue("Expected file names must match the produced file names", CollectionUtils.isEqualCollection(expectedFiles.keySet(), actualFiles.keySet()));
      expectedFiles.forEach((key, expectedFile) -> {
        File actualFile = actualFiles.get(key);
        try {
          compareFiles(expectedFile, actualFile);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private void assertLogResultContains(List<String> lines, String... messages) {
    Set<String> search = new HashSet<>(Arrays.asList(messages));
    for (String line : lines) {
      search = search.stream().filter(message -> line.indexOf(message) < 0).collect(Collectors.toSet());
    }
    assertEquals("Expected to find messages " + search.stream().collect(Collectors.joining(", ")), search.size());
  }

  private void compareFiles(File expectedFile, File actualFile) throws IOException {
    List<String> expectedLines = FileUtils.readLines(expectedFile, StandardCharsets.UTF_8);
    List<String> actualLines = FileUtils.readLines(actualFile, StandardCharsets.UTF_8);
    assertTrue("File with expected content does not exist: '" + actualFile.getAbsolutePath() + "'", expectedFile.exists());
    assertTrue("File with produced content does not exist: '" + actualFile.getAbsolutePath() + "'", actualFile.exists());
    // Ignore empty lines
    expectedLines.removeIf(StringUtils::isBlank);
    actualLines.removeIf(StringUtils::isBlank);
    // Check file contents
    assertTrue("Expected file must contain at least one non-empty line: '" + actualFile.getAbsolutePath() + "'", expectedLines.size() > 0);
    assertEquals("Number of non-empty lines in expected file must match the generated number of lines: '" + actualFile.getAbsolutePath() + "'",
        expectedLines.size(), actualLines.size());
    for (int i = 0, j = expectedLines.size(); i < j; ++i) {
      assertEquals("Actual content of file '" + actualFile.getAbsolutePath() + "' differs from the expected content", expectedLines.get(i).trim(), actualLines.get(i).trim());
    }
  }

  private Map<String, File> listFiles(File basedir) {
    return FileUtils.listFiles(basedir, null, true).stream().collect(Collectors.toMap(file -> {
      try {
        return FileHelper.relativizePath(basedir, file);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, Function.identity()));
  }

  private void clean(File basedir) throws IOException {
    File target = new File(basedir, "target");
    if (target.exists()) {
      FileUtils.forceDelete(target);
    }
    assertFalse(target.exists());
  }

  protected interface Action {
    void run() throws Throwable;
  }

  private static class MavenResult {
    final String outString;
    final String errString;

    public MavenResult(String outString, String errString) {
      this.outString = outString;
      this.errString = errString;
    }

    public String getOutString() {
      return outString;
    }

    public String getErrString() {
      return errString;
    }
  }
}
