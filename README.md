vertx-stack
========

[![Build Status (5.x)](https://github.com/vert-x3/vertx-stack/actions/workflows/ci-5.x.yml/badge.svg)](https://github.com/vert-x3/vertx-stack/actions/workflows/ci-5.x.yml)
[![Build Status (4.x)](https://github.com/vert-x3/vertx-stack/actions/workflows/ci-4.x.yml/badge.svg)](https://github.com/vert-x3/vertx-stack/actions/workflows/ci-4.x.yml)

The Vert.x stack : Vert.x + the endorsed modules

### Maven

This project provides pre-configured Maven poms for using in your projects, allowing you to consume the Vert.x stack
easily.

#### Dependency chain (_Maven depchain_)

This artifact `io.vertx:stack-depchain` is a POM projects can import to get the dependencies it needs for running
the base stack:

~~~~
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-stack-depchain</artifactId>
  <version>3.5.1</version>
  <type>pom</type>
</dependency>
~~~~

#### Bills of Materials (Maven _BOM_)

A [_BOM_](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html) is a also a POM you
can import in your project. It will not add dependencies to your POM, instead it will set the correct versions to use.
Therefore it should be used with explicit dependencies:

~~~~
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-stack-depchain</artifactId>
      <version>3.5.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-core</artifactId>
    </dependency>
    ...
  </dependencies>
~~~~

### Adding a new module to the stack

1. Add it to [`vertx-dependencies`](https://github.com/vert-x3/vertx-dependencies) (open the project, add it to the `pom.xml`, install, commit and push)
2. Add it to `stack-depchain/pom.xml` (open the project, add the dependency, without the version (inherited from
vertx-dependencies))
3. Add it in the stack manager:
  * Open `./stack-manager/src/main/descriptor/vertx-stack-full.json`
  * Add the dependency. If the dependency must be embedded in the _full_ distribution, set `included` to `true`. Don't forget to use the `\${version}`.
4. Add it to the doc distribution:
  * Open `./stack-docs/pom.xml`
  * Add the `docs` dependency (the using `<classifier>docs</classifier>` and `<type>zip</type>`)
  * Add the `source` dependency (the `<classifier>sources</classifier>`)
  * Add the _copy_ instruction for the ant execution:
```
<copy todir="${project.build.directory}/docs/vertx-hawkular-metrics/">
    <fileset dir="${project.build.directory}/work/vertx-hawkular-metrics-docs-zip"/>
</copy>
```
  * Save the `pom.xml` file
  * Build (`mvn clean install` from the root).
5. Make it polyglot  (unless the module is not polyglot, e.g. a cluster manager)
  * Kotlin: in [`vertx-lang-kotlin`](https://github.com/vert-x3/vertx-lang-kotlin), edit `vertx-lang-kotlin/pom.xml`
    * Add an `optional` dependency to the POM
    * Update the list in the `maven-dependency-plugin` config
  * RxJava3: in [`vertx-rx`](https://github.com/vert-x3/vertx-rx), edit `rx-java3/pom.xml`
    * Add an `optional` dependency to the POM
    * Update the list in the `maven-dependency-plugin` config
  * RxJava2: in [`vertx-rx`](https://github.com/vert-x3/vertx-rx), edit `rx-java2/pom.xml`
    * Add an `optional` dependency to the POM
    * Update the list in the `maven-dependency-plugin` config
6. Add it to the website in [`vertx-web-site`](https://github.com/vertx-web-site/vertx-web-site.github.io):
  * Edit the docs summary page `docs/metadata/<version>.jsx`, use `Tech Preview` label
7. Add it to the starter website in [`vertx-starter`](https://github.com/vert-x3/vertx-starter/):
  * Edit the stack definition file `src/main/resources/starter.json`
    * Add the module details in one of the categories (web, data, ...etc)
    * Exclude the module from versions that did not have it (`exclusions`)
