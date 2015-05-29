# A base Dockerfile for Vert.x 3

FROM java:8

MAINTAINER Julien Viet <julien@julienviet.com>

COPY ./usr /usr/

# Set path
ENV VERTX_HOME /usr/local/vert.x-${project.version}
ENV PATH $VERTX_HOME/bin:$PATH
