# A base Dockerfile for Vert.x 3 preview

# Use latest Fedora image as the base
FROM fedora
MAINTAINER Julien Viet <julien@julienviet.com>

# Install java 8, wget, and git
RUN yum -q -y install java-1.8.0-openjdk-devel.x86_64 wget git && yum -q clean all

# Set JAVA_HOME
ENV JAVA_HOME /usr/

# Install maven
RUN wget http://wwwftp.ciril.fr/pub/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
RUN tar -zxvf apache-maven-3.0.5-bin.tar.gz -C /usr/local && rm apache-maven-3.0.5-bin.tar.gz

# Clone and install the codegen repository
RUN git clone --depth 1 https://github.com/vert-x3/codegen /home/work/codegen
RUN cd /home/work/codegen && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vert.x repository
RUN git clone --depth 1 https://github.com/eclipse/vert.x /home/work/vert.x
RUN cd /home/work/vert.x && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true -Pdist install

# Clone and install the vertx-js repository
RUN git clone --depth 1 https://github.com/vert-x3/vertx-js /home/work/vertx-js
RUN cd /home/work/vertx-js && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-groovy repository
RUN git clone --depth 1 https://github.com/vert-x3/vertx-groovy /home/work/vertx-groovy
RUN cd /home/work/vertx-groovy && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-stack repository
RUN git clone --depth 1 https://github.com/vert-x3/vertx-stack /home/work/vertx-stack
RUN cd /home/work/vertx-stack && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Copy binary
RUN tar -xvzf /home/work/vertx-stack/stack-dist/target/vertx-stack-dist-3.0.0-SNAPSHOT.tar.gz -C /usr/local
ENV VERTX_HOME /usr/local/vert.x-3.0.0-SNAPSHOT
ENV PATH $VERTX_HOME/bin:$PATH

# Cleanup
RUN rm -rf /home/work /root/.m2 /usr/local/apache-maven-3.0.5
