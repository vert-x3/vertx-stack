# A base Dockerfile for Vert.x 3

FROM openjdk:8u171-jre

MAINTAINER Clement Escoffier <clement@apache.org>

# Install the ps command to get the Launcher 'stop' command
# working properly
RUN apt-get update && apt-get install -y procps
COPY ./ /usr/local/
RUN chmod +x /usr/local/vertx/bin/vertx

# Set path
ENV VERTX_HOME /usr/local/vertx
ENV PATH $VERTX_HOME/bin:$PATH
