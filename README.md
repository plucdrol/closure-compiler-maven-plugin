# Closure Compiler Maven Plugin

Forked from [Minify Maven Plugin](http://samaxes.github.io/minify-maven-plugin/). That project seems to be inactive. In line with the principle of single
responsibility, this fork is meant only for processing JavaScript files. The [YUI Compressor](http://yui.github.com/yuicompressor/) is dead, so what remains
is a maven plugin for [Google Closure Compiler](https://developers.google.com/closure/compiler/). I found some plugins for the closure compiler, but found
them all to be lacking. So I decided to fork the excellent Minify Maven Plugin as a base for a closure compiler maven plugin.

This plugin combines and minimizes JavaScript files. It produces a merged and a minified version.

Requires at least Java 1.8.

# Usage

Configure your project's `pom.xml` to run the plugin during the project's build cycle.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.blutornage</groupId>
      <artifactId>closure-compiler-maven-plugin</artifactId>
      <version>2.1.1</version>
      <executions>
        <execution>
          <id>default-minify</id>
          <configuration>
            <encoding>UTF-8</encoding>
            <includes>
              <include>file-1.js</include>
              <!-- ... -->
              <include>file-n.js</include>
            </includes>
          </configuration>
          <goals>
            <goal>minify</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

For more information, check the [documentation](http://blutorange.github.com/closure-compiler-maven-plugin/)
or the [demo applications](https://github.com/blutorange/closure-compiler-maven-plugin/tree/master/demo).

# Build site

* Edit files in /src/site
* To upload to github, set `dryRun` in `pom.xml` to `false`.
* `mvn clean plugin:report site`

If `dryRun` is set to `true`, you can check out the site in `target/site/index.html`.

# Release

* `mvn versions:display-dependency-updates`
* Update version in `pom.xml` and `src/test/resources/projects/parent/pom.xml`.
* Update version in `site/src/*` and `README.md`.
* Update CHANGELOG.md
* Generate site, check links
* Upload site to github (see above)
* Upload source to github
* `mvn clean install`
* `mvn -P release deploy`

# Test

The test projects need a built version of the plugin, so make a full local build first:

```sh
mvn clean install -DskipTests
```

You may need to run an install on a test project first to download the required dependencies:

```sh
cd src/test/resources/projects/minimal/
mvn install
cd ../../../../../
```

Now test away

```sh
mvn clean package test
```

To add a new test, go to `src/test/resources/projects/`, copy one of the test projects as a base (except `parent`). Open the pom.xml
and change the `artifactId` to a new name. Edit the closure compiler configuration as necessary. Add input JavaScript files to
the directory `test` and the expected output files to the directory `expected`. Finally open `MinifyMojoTest` and add a new test method:

```java
  @Test
  public void testMyproject() throws Exception {
    runMinify("myproject");
  }
```

# License

This distribution is licensed under the terms of the Apache License, Version 2.0 (see LICENSE.txt).
