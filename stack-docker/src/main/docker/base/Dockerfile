# A base Dockerfile for Vert.x 3

FROM openjdk:8-jre-alpine

MAINTAINER Clement Escoffier <clement@apache.org>

COPY ./usr /usr/
RUN apk add --update bash && rm -rf /var/cache/apk/*
RUN chmod +x /usr/local/vertx/bin/vertx

# Set path
ENV VERTX_HOME /usr/local/vertx
ENV PATH $VERTX_HOME/bin:$PATH
