vertx-stack
========

[![Build Status](https://vertx.ci.cloudbees.com/buildStatus/icon?job=vert.x3-stack)](https://vertx.ci.cloudbees.com/view/vert.x-3/job/vert.x3-stack/)

The Vert.x stack : Vert.x + the endorsed modules

### Distributions

#### Vert.x min

- Vert.x Core
- Groovy, JS and Ruby languages
- Hazelcast clustering
- Service proxy

#### Vert.x base

Based on Vert.x min:

- Reactive programming
- Services deployment
- Vert.x Unit
- Dropwizard Metrics

#### Vert.x full

Based on Vert.x base:

- Auth + Web components
- Data components
- Mail service

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
  <version>3.0.0-SNAPSHOT</version>
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
      <artifactId>stack-bom</artifactId>
      <version>3.0.0-SNAPSHOT</version>
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

The images can be pushed to Docker Hub. Before, be sure you are in the _vertx_ organisation on docker hub (https://registry.hub.docker.com/repos/vertx/). Then, add your credentials into `~/.m2/settings.xml`:

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

