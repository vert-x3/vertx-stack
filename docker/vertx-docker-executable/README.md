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