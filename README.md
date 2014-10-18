vertx-stack
========

The Vert.x stack : Vert.x + the endorsed modules

### Distributions

- [zip snapshot](https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=io.vertx&a=vertx-stack-dist&v=3.0.0-SNAPSHOT&e=zip)
- [tgz snapshot](https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=io.vertx&a=vertx-stack-dist&v=3.0.0-SNAPSHOT&e=tar.gz)

### Maven

This project provides preconfigured Maven poms for using in your projects, allowing you to consume the Vert.x stack
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

To build docker images, you need docker1.3+ and run docker-build-images.sh
It will build two images :
- vertx3
- vertx3-exec

#### Classical image

The eclipse/vertx3 is the previous refactored image with official java 8 image from Docker (since 1.3+)
An environment variabe
