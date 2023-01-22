package dev.suresh.domain;

import java.util.Objects;

public record JVersion(String name, String version) {
  public JVersion {
    Objects.requireNonNull(name, "Name is required");
    Objects.requireNonNull(version, "Version is required");
  }

  public JVersion() {
    this("Java", System.getProperty("java.version"));
  }
}
