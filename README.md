vertx-stack
========

[![Build Status](https://travis-ci.org/vert-x3/vertx-stack.svg?branch=master)](https://travis-ci.org/vert-x3/vertx-stack)

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
  <artifactId>stack-depchain</artifactId>
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

### Docker

To build docker images, you need docker1.3+ and run `mvn clean install -Pdocker`
It builds the 2 main images :
- vert.x3 base - the base image provisionning the vert.x stack (`vertx/vertx3`)
- vert.x3 executable - an image providing the vert.x command (`vertx/vertx3-executable`)

All images have a _readme_ file containing their documentation and build instructions.

Examples of docker image usages are in https://github.com/vert-x3/vertx-examples/docker-examples

#### Pushing Docker image to Docker Hub

The images can be pushed to Docker Hub. Before, be sure you are in the _vertx_ organisation on docker hub (https://hub.docker.com/u/vertx/). Then, add your credentials into `~/.m2/settings.xml`:

```
<server>
  <id>docker-hub</id>
  <username>username</username>
  <password>password</password>
  <configuration>
    <email>email</email>
  </configuration>
</server>
```

Once done, in the `vertx-docker-base-image` and `vertx-docker-executable` project just launch:

```
mvn docker:push
```

**WARNING**: This is going to take a while.....

### Adding a new module to the stack

1. Add it to `vertx-dependencies` (open the project, add it to the `pom.xml`, install, commit and push)
2. Add it to `stack-depchain/pom.xml` (open the project, add the dependency, without the version (inherited from
vertx-dependencies))
3. Add it in the stack manager:
  * Open `./stack-manager/src/main/descriptor/vertx-stack.json`
  * Add the dependency. If the dependency must be embedded in the _min_ distribution, set `included` to `true`. So forget to use the `\${version}`.
  * Open `./stack-manager/src/main/descriptor/vertx-stack-full.json`
  * Add the dependency. If the dependency must be embedded in the _full_ distribution, set `included` to `true`. So forget to use the `\${version}`.
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
4. Build (`mvn clean install` from the root).


