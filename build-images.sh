#!/bin/bash

set -e
set -x

cd images
docker build -t eclipse/vertx3 .

cd executable
docker build -t eclipse/vertx3-exec .

cd ../..
