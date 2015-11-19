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

package io.vertx.stack.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class LocalDependency {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private String type;
  private List<Exclusion> exclusions = new ArrayList<>();

  private String scope = "compile";
  private boolean optional = false;
  private String classifier;

  public LocalDependency(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    Objects.requireNonNull(groupId);
    Objects.requireNonNull(artifactId);
    this.version = version;
  }

  public LocalDependency(String groupId, String artifactId) {
    this(groupId, artifactId, null);
  }

  public LocalDependency type(String type) {
    this.type = type;
    return this;
  }

  public LocalDependency classifier(String c) {
    this.classifier = c;
    return this;
  }

  public LocalDependency optional(boolean opt) {
    this.optional = opt;
    return this;
  }

  public LocalDependency scope(String scope) {
    this.scope = scope;
    return this;
  }

  public LocalDependency addExclusion(String groupId, String artifactId) {
    this.exclusions.add(new Exclusion(groupId, artifactId));
    return this;
  }

  private class Exclusion {

    private final String groupId;
    private final String artifactId;

    public Exclusion(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }
  }

  public String toDependency(int indent) {
    StringBuilder builder = new StringBuilder();
    builder.append(PomUtils.indentation(indent)).append("<dependency>\n");
    builder.append(PomUtils.indentation(indent + 1)).append("<groupId>").append(groupId).append("</groupId>\n");
    builder.append(PomUtils.indentation(indent + 1)).append("<artifactId>").append(artifactId).append("</artifactId>\n");
    if (version != null) {
      builder.append(PomUtils.indentation(indent + 1)).append("<version>").append(version).append("</version>\n");
    }
    if (optional) {
      builder.append(PomUtils.indentation(indent + 1)).append("<optional>true</optional>\n");
    }
    if (type != null) {
      builder.append(PomUtils.indentation(indent + 1)).append("<type>").append(type).append("</type>\n");
    }
    if (scope != null) {
      builder.append(PomUtils.indentation(indent + 1)).append("<scope>").append(scope).append("</scope>\n");
    }
    if (classifier != null) {
      builder.append(PomUtils.indentation(indent + 1)).append("<classifier>").append(classifier).append("</classifier>\n");
    }
    if (! exclusions.isEmpty()) {
      builder.append(PomUtils.indentation(indent + 1)).append("<exclusions>\n");
      for (Exclusion exclusion : exclusions) {
        builder.append(PomUtils.indentation(indent + 2)).append("<exclusion>\n");
        builder.append(PomUtils.indentation(indent + 3)).append("<groupId>").append(exclusion.groupId).append("</groupId>\n");
        builder.append(PomUtils.indentation(indent + 3)).append("<artifactId>").append(exclusion.artifactId).append("</artifactId>\n");
        builder.append(PomUtils.indentation(indent + 2)).append("</exclusion>\n");
      }
      builder.append(PomUtils.indentation(indent + 1)).append("</exclusions>\n");
    }
    builder.append(PomUtils.indentation(indent)).append("</dependency>\n");
    return builder.toString();
  }
}
