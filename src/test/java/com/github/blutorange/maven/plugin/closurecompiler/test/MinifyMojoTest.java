package com.github.blutorange.maven.plugin.closurecompiler.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;

public class MinifyMojoTest {

  @Rule
  public TestResources testResources = new TestResources("src/test/resources/projects", "target/test-projects");

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testMinimal() throws Exception {
    runMinify("minimal");
  }

  private void runMinify(String projectName) throws Exception {
    File basedir = testResources.getBasedir(projectName).getCanonicalFile();
    File pom = new File(basedir, "pom.xml");
    assertTrue(pom.exists());

    clean(basedir);
    invokeMaven(pom);
    assertDirContent(basedir);
  }

  private void invokeMaven(File pom) throws IOException {
    MavenCli cli = new MavenCli();
    String[] args = new String[] { "clean", "com.github.blutorange:closure-compiler-maven-plugin:minify" };
    System.setProperty("maven.multiModuleProjectDirectory", pom.getParent());
    cli.doMain(args, pom.getParent(), System.out, System.err);
  }

  private void assertDirContent(File basedir) {
    File expected = new File(basedir, "expected");
    File actual = new File(new File(basedir, "target"), "test");
    assertTrue(expected.exists());
    assertTrue(actual.exists());
    Map<String, File> expectedFiles = listFiles(expected);
    Map<String, File> actualFiles = listFiles(actual);
    assertTrue(expectedFiles.size() > 0);
    assertEquals(expectedFiles.size(), actualFiles.size());
    assertTrue(CollectionUtils.isEqualCollection(expectedFiles.keySet(), actualFiles.keySet()));
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

  private void compareFiles(File expectedFile, File actualFile) throws IOException {
    List<String> expectedLines = FileUtils.readLines(expectedFile, StandardCharsets.UTF_8);
    List<String> actualLines = FileUtils.readLines(actualFile, StandardCharsets.UTF_8);
    assertTrue(expectedFile.exists());
    assertTrue(actualFile.exists());
    // Ignore empty lines
    expectedLines.removeIf(StringUtils::isBlank);
    actualLines.removeIf(StringUtils::isBlank);
    // Check file contents
    assertTrue(expectedLines.size() > 0);
    assertEquals(expectedLines.size(), actualLines.size());
    for (int i = 0, j = expectedLines.size(); i < j; ++i) {
      assertEquals(expectedLines.get(i).trim(), actualLines.get(i).trim());
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
}