# A base Dockerfile for Vert.x 3

FROM java:8u72-jre

MAINTAINER Clement Escoffier <clement@apache.org>

COPY ./usr /usr/
RUN chmod +x /usr/local/vertx/bin/vertx

# Set path
ENV VERTX_HOME /usr/local/vertx
ENV PATH $VERTX_HOME/bin:$PATH
