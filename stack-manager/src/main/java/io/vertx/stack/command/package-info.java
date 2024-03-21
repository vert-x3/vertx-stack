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

/**
 * == Stack Manager
 *
 * The stack manager is a tool to manage your vert.x distribution. It maintains the set of jar files contained in the
 * `lib` directory. The stack manager takes as input a YAML file describing the _dependencies_ that should be
 * included in the stack. Then, during the resolution it resolves new dependencies and delete the files that are not
 * used anymore.
 *
 * If you have downloaded a vert.x distribution, it comes with a `vertx-stack.json` in the root directory. This file
 * contains all the official dependencies you can add. By default it _includes_ only the minimal stack.
 *
 * === Adding artifacts
 *
 * To add an artifact to your stack, just add the dependency if not there already, and set the `included` value to
 * `true`:
 *
 * [source]
 * ----
 * {
 *  "groupId": "io.vertx",
 *  "artifactId": "vertx-sync",
 *  "version": "${vertx.version}",
 *  "included": true
 * }
 * ----
 *
 * The dependency is described using the Maven coordinates. The `groupId`, `artifactId` and `versions` attributes are
 * mandatory. You can also set the `type` (`jar` by default) and `classifier` (none by default).
 *
 * === Removing artifacts
 *
 * To remove an artifact, remove the dependency or set the `included` attribute to `false`:
 *
 * [source]
 * ----
 * {
 *  "groupId": "io.vertx",
 *  "artifactId": "vertx-sync",
 *  "version": "${vertx.version}",
 *  "included": false
 * }
 * ----
 *
 * During the resolution, all files that are not associated to a dependency (or a transitive dependency) are removed.
 * That means don't add files manually, they are going to be deleted.
 *
 * === Launching the resolution
 *
 * To launch the resolution, launch:
 *
 * [source]
 * ----
 * ./bin/vertx resolve --dir=lib
 * ----
 *
 * The `resolve` command supports a set of options:
 *
 * *  `--dir &lt;value&gt;` - The directory containing the artifacts composing the stack. Defaults to the `./lib`
 * directory
 * *  `--fail-on-conflict`  - Set whether or not the resolver should fail or conflict or just log a warning. Disabled by default.
 * * `--http-proxy &lt;value&gt;` - Set the HTTP proxy address if any.
 * * `--https-proxy &lt;value&gt;` - Set the HTTPS proxy address if any.
 * * `--local-repo &lt;value&gt;` - Set the path to the local Maven repository. Defaults to `~/.m2/repository`
 * * `--remote-repo &lt;value&gt;` -  Set the path to a remote Maven repository. Can be set multiple times
 * * `&lt;vertx-stack.json&gt;` - The path to the stack descriptor. Defaults to `vertx-stack.json`.
 * * `--no-cache` -  Disable the resolver cache
 * * `--no-cache-for-snapshots` - Disable the caching of snapshot resolution
 *
 * If you have set `VERTX_HOME` as environment variable (or system variable), it uses: `$VERTX_HOME/lib` and
 * `$VERTX_HOME/vertx-stack.json`.
 *
 * === Exclusions and Transitives
 *
 * Each dependency can declare a set of exclusions that won't be resolved during the resolution process:
 *
 * [source]
 * ----
 * {
 *  "groupId": "org.acme",
 *  "artifactId": "acme-lib",
 *  "version": "1.0.0",
 *  "included": true,
 *  "exclusions": [{
 *    "groupId": "org.acme",
 *    "artifactId": "acme-not-required"
 *  }]
 * }
 * ----
 *
 * You can also set the `transitive` attribute to `false` on a dependency to not resolve the transitive dependencies.
 * This is useful when using fat jars or shaded artifacts:
 *
 * [source]
 * ----
 * {
 *  "groupId": "io.vertx",
 *  "artifactId": "vertx-web-templ-thymeleaf",
 *  "version": "${vertx.version}",
 *  "included": true,
 *  "classifier": "shaded",
 *  "transitive": false
 * }
 * ----
 *
 * === Using variables
 *
 * The stack definition allows you to declare variables:
 *
 * [source]
 * ----
 * {
 * "variables": {
 *   "vertx.version": "4.5.7-SNAPSHOT"
 * }
 * ----
 *
 * Then your dependency can uses this variable using the `${}` notation.
 *
 * Variables can be set or overridden using system variables. System variables can be set with `-D`
 *
 * === A note about the JSON format
 *
 * The descriptor format supports:
 *
 * * comments using `//`
 * * non quoted keys (such as `groupId : "org.acme"`)
 * * single quotes for values (such as `groupId : 'org.acme'`)
 *
 */
@Document(fileName = "index.adoc")
package io.vertx.stack.command;

import io.vertx.docgen.Document;
