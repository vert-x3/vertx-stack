# Vert.x Executable from Docker

This projects builds a Docker image providing the `vertx` command
 
# Build the image

To build the docker image, just launch:

`mvn clean package`

Notice that you need to have docker installed on your machine.

# Launching the image

Just launch:
 
`docker run -i -t vertx/vertx3-executable`

Append the `vertx` command parameter you need.

for instance:

```
> docker run -i -t vertx/vertx3-executable -version
3.0.0-SNAPSHOT 
```

If you want to run a verticle:

```
docker run -i -t -p 8080:8080 \ 
    -v $PWD:/verticles vertx/vertx3-executable \  
    run io.vertx.sample.RandomGeneratorVerticle \ 
    -cp /verticles/MY_VERTICLE.jar 
```

This command mount the current directory into `/verticles` and then launch the `vertx run` command. Notice the `-cp` 
parameter reusing the `/verticles` directory.
