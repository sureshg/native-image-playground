package dev.suresh.model;

import static java.util.Objects.requireNonNull;

public record JVersion(String name, String version) {
  public JVersion {
    requireNonNull(name, "Name is required");
    requireNonNull(version, "Version is required");
  }

  public JVersion() {
    this("Java", System.getProperty("java.version"));
  }
}
