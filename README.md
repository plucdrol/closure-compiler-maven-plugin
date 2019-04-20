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
  
  <!-- Exclude the resources in the "includes" directory -->
  <!-- Include the transpiled resources in the target directory instead -->
	<resources>
		<resource>
			<directory>src/main/resources</directory>
			<excludes>
				<exclude>includes/**/*.js</exclude>
			</excludes>
		</resource>
		<resource>
			<directory>${project.basedir}/target/generated-resources</directory>
		</resource>
	</resources>
  
  <!-- Transpiled all sources in the "includes" directory to the target directory -->
  <plugins>
    <plugin>
      <groupId>com.github.blutorange</groupId>
      <artifactId>closure-compiler-maven-plugin</artifactId>
      <version>2.4.0</version>
				<configuration>
					<!-- Base configuration for all executions (bundles) -->
					<baseSourceDir>${project.basedir}/src/main/resources</baseSourceDir>
					<baseTargetDir>${project.build.directory}/generated-resources</baseTargetDir>
				</configuration>
				<executions>
					<execution>
						<id>default-minify</id>
						<configuration>
							<!-- Process all files in the "includes" directory individually-->
							<encoding>UTF-8</encoding>
							<sourceDir>includes</sourceDir>
							<targetDir>includes</targetDir>
							<includes>**/*.js</includes>
							<skipMerge>true</skipMerge>
							<closureLanguageOut>ECMASCRIPT5</closureLanguageOut>
						</configuration>
						<goals>
							<goal>minify</goal>
						</goals>
						<phase>generate-resources</phase>
					</execution>
				</executions>
    </plugin>
  </plugins>
</build>
```

# Documentation

For more information, check the [documentation](http://blutorange.github.com/closure-compiler-maven-plugin/)
or the [test projects](https://github.com/blutorange/closure-compiler-maven-plugin/tree/master/src/test/resources/projects).

# Build site

* Edit files in /src/site
* To upload to github, set add the profile `site`
* `mvn clean plugin:report site`

If rendering the site locally, you can check out the rendered site in `target/site/index.html`.

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
