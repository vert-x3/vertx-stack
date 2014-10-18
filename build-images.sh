#!/bin/bash

set -e
set -x

cd images
docker build -t vertx/vertx3 .

cd executable
docker build -t vertx/vertx3-exec .

cd ../..
