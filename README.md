** WORK-IN-PROGRESS, snapshot release available **

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
      <version>2.0.0</version>
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

# Release

* Update version in `pom.xml` and `demo/pom.xml`.
* Update version in `site/src/*`.
* Generate site, check links
* Upload site to github
* `mvn clean install`
* `mvn -P release deploy`

# License

This distribution is licensed under the terms of the Apache License, Version 2.0 (see LICENSE.txt).
