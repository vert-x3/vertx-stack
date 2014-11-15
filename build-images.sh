#!/bin/bash

set -e
set -x

cd images
docker build --rm=true -t vertx/vertx3 .

cd executable
docker build --rm=true -t vertx/vertx3-exec .

cd ../..
