import io.vertx.core.impl.launcher.commands.VersionCommand

/*
 *  Copyright (c) 2011-2015 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

vertx.createHttpServer().requestHandler({ req ->

  if (req.path() == "/") {
    req.response()
            .putHeader("content-type", "text/html")
            .end("<html><body><h1>Hello from vert.x!</h1></body></html>")
  } else if (req.path() == "/version") {
    String version = new VersionCommand().version;
    req.response()
            .putHeader("content-type", "text/plain")
            .end(version)
  }

}).listen(8080)