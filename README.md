vertx-stack
========

The Vert.x stack : Vert.x + the endorsed modules

### Distributions

- [zip snapshot](https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=io.vertx&a=vertx-stack-dist&v=3.0.0-SNAPSHOT&e=zip)
- [tgz snapshot](https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=io.vertx&a=vertx-stack-dist&v=3.0.0-SNAPSHOT&e=tar.gz)
- [Docker base image](https://registry.hub.docker.com/u/vertx/vertx3/)
- [Docker executable image](https://registry.hub.docker.com/u/vertx/vertx3-exec/)

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
It builds four images :
- vert.x3 base - the base image provisionning the vert.x stack (`vertx/vertx3`)
- vert.x3 executable - an image providing the vert.x command (`vertx/vertx3-executable`)
- vert.x3 docker example - an example of verticle using the base image (`vertx/vertx3-example`)
- vert.x3 docker example for fabric 8 - an example of verticle that you can deploy on fabric8 
(`vertx/vertx3-example-fabric8`) 

All images have a _readme_ file containing their documentation and build instructions.


