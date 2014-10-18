#!/bin/bash

set -e
set -x

cd base
docker build -t eclipse/vertx3 .

cd executable
docker build -t eclipse/vertx3-exec .

cd ../..
