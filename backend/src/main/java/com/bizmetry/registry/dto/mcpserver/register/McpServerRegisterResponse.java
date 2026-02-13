package com.bizmetry.registry.dto.mcpserver.register;

import java.util.UUID;

public class McpServerRegisterResponse {

  private UUID id;
  private String name;
  private String version;

  public McpServerRegisterResponse(UUID id, String name, String version) {
    this.id = id;
    this.name = name;
    this.version = version;
  }

  // Getters y setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
