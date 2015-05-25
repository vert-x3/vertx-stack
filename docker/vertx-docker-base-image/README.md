# Vert.x Stack Docker Image

This project builds the base docker image for the vert.x 3 stack. The built image contains the `vertx` command in the
 system path.
 
# Build the image

To build the docker image, just launch:

`mvn clean install`

Notice that you need to have docker installed on your machine.

# Using the image

The image is intended to be used by extension using the Docker `FROM` directive. Here is an example:

```
FROM vertx/vertx3

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Set the name of the verticle to deploy
ENV VERTICLE_NAME io.vertx.example.HelloWorldVerticle

# Set vertx option
ENV VERTX_OPTIONS ""

###
# The rest of the file should be fine.
###

COPY ./verticles $VERTICLE_HOME

# We use the "sh -c" to turn around https://github.com/docker/docker/issues/5509 - variable not expanded
ENTRYPOINT ["sh", "-c"]
CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/* $VERTX_OPTIONS"]
```

# Launching the image

The resulting image is not made to be launched directly (as it contains only vert.x and no applications). If you 
still want to launch it uses:
 
`docker run -i -t vertx/vertx3`

The vert.x files are located in ` /usr/local/vert.x-3.0.0-SNAPSHOT/`.

You can access the `vertx` command directly using:

`docker run -i -t vertx/vertx3 vertx`