# A base Dockerfile for Vert.x 3 preview

# Use latest Official Java 8 Image
# based onto dockerfile/ubuntu
FROM dockerfile/java:oracle-java8

MAINTAINER Julien Viet <julien@julienviet.com>

# Choose the git branch to clone
ENV GIT_BRANCH -b master

# Add the cripts to build the vertx source
ADD scripts/ /home/work/scripts
RUN chmod -R +x /home/work/scripts
RUN /home/work/scripts/install.sh

# Copy binary
ENV VERTX_HOME /usr/local/vert.x-3.0.0-SNAPSHOT
ENV PATH $VERTX_HOME/bin:$PATH
