package io.vertx.stack.builder.utils;

import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some utilities methods to manage artifacts.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Artifacts {

  /**
   * Check whether or not the given file name is a 'snapshot' version of the given {@link Artifact}. This method
   * ignores the version, and just check if the file correspond to a snapshot version for the given artifact id. this
   * method supports snapshots using timestamps.
   *
   * @param artifact the artifact
   * @param fileName the file name
   * @return {@code true} if the file name is a snapshot version of the artifact, {@link false} otherwise.
   */
  public static boolean isSnapshot(Artifact artifact, String fileName) {
    if (fileName.endsWith("-SNAPSHOT"
        + (artifact.getClassifier().length() != 0 ? "-" + artifact.getClassifier() : "")
        + "." + artifact.getExtension())) {
      return true;
    }
    Pattern pattern = Pattern.compile(artifact.getArtifactId()
        + "-(.*)-[0-9]{8}\\.[0-9]{6}-[0-9]+"
        + (artifact.getClassifier().length() != 0 ? "-" + artifact.getClassifier() : "")
        + "\\." + artifact.getExtension());
    Matcher matcher = pattern.matcher(fileName);
    return matcher.find();
  }

  /**
   * Checks whether the given file is a file from the given artifact. This method is not perfect it may have false
   * positive.
   *
   * @param artifact the artifact
   * @param fileName the file name
   * @return {@code true} if the file name is correspond to the artifact, {@link false} otherwise.
   */
  public static boolean matchesArtifact(Artifact artifact, String fileName) {
    String prefix = artifact.getArtifactId() + "-";
    String suffix = (artifact.getClassifier().length() != 0 ? "-"
        + artifact.getClassifier() : "") + "." + artifact.getExtension();
    return fileName.startsWith(prefix) &&
        fileName.endsWith(suffix) &&
        // Check that the 'version' starts with a digit.
        Character.isDigit(fileName.substring(prefix.length()).charAt(0));
  }

  /**
   * Gets the normalized version extracted from the given file name associated with the given artifact.
   *
   * @param artifact the artifact
   * @param fileName the file name
   * @return the normalized snapshot version. If the file name using `-SNAPSHOT` return the version. If the file name
   * use a timestamp, it returns the `-SNAPSHOT` version. If the file name is a release, {@code null} is returned.
   */
  public static String getSnapshotVersion(Artifact artifact, String fileName) {
    if (fileName.contains("-SNAPSHOT")) {
      return fileName.substring(artifact.getArtifactId().length() + 1, fileName.indexOf("-SNAPSHOT")) + "-SNAPSHOT";
    }
    Pattern pattern = Pattern.compile(artifact.getArtifactId()
        + "-(.*)-[0-9]{8}\\.[0-9]{6}-[0-9]+"
        + (artifact.getClassifier().length() != 0 ? "-" + artifact.getClassifier() : "")
        + "\\." + artifact.getExtension());
    Matcher matcher = pattern.matcher(fileName);
    if (matcher.find()) {
      return matcher.group(1) + "-SNAPSHOT";
    }
    return null;
  }

  /**
   * Gets the file corresponding to the given artifact installed in the given directory.
   *
   * @param directory the directory
   * @param artifact  the artifact
   * @return the file, {@code null} if not found
   */
  public static File getArtifactFile(File directory, Artifact artifact) {
    File[] files = directory.listFiles((dir, name) -> {
      return matchesArtifact(artifact, name);
    });
    if (files == null || files.length == 0) {
      return null;
    } else {
      return files[0];
    }
  }

  public static String getFileName(Artifact artifact) {
    return artifact.getArtifactId() + "-" + artifact.getBaseVersion()
        + (artifact.getClassifier() != null && artifact.getClassifier().length() > 0 ? "-" + artifact.getClassifier() : "")
        + "." + artifact.getExtension();
  }
}
