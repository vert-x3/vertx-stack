# Vert.x Stack Docker Images

This project builds the docker images for the Vert.x 3 stack.

## Build the images

To build the docker images, just launch:

`mvn clean install`

Notice that you need to have docker installed on your machine.

## Vert.x Stack docker base Image

The built image contains the `vertx` command in the system path.
 
## Using the base image

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

## Launching the base image

The resulting image is not made to be launched directly (as it contains only vert.x and no applications). If you 
still want to launch it uses:
 
`docker run -i -t vertx/vertx3`

The vert.x files are located in ` /usr/local/vert.x-3.0.0-SNAPSHOT/`.

You can access the `vertx` command directly using:

`docker run -i -t vertx/vertx3 vertx`

## Vert.x executable image from Docker

A Docker image providing the `vertx` command

## Launching the executable image

Just launch:

`docker run -i -t vertx/vertx3-exec`

Append the `vertx` command parameter you need.

for instance:

```
> docker run -i -t vertx/vertx3-exec -version
3.0.0-SNAPSHOT
```

If you want to run a verticle:

```
docker run -i -t -p 8080:8080 \
    -v $PWD:/verticles vertx/vertx3-exec \
    run io.vertx.sample.RandomGeneratorVerticle \
    -cp /verticles/MY_VERTICLE.jar
```

This command mount the current directory into `/verticles` and then launch the `vertx run` command. Notice the `-cp`
parameter reusing the `/verticles` directory.
