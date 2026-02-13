package com.bizmetry.registry.dto.mcpserver.register;

import jakarta.validation.constraints.NotBlank;

public class McpServerRegisterRequest {

  @NotBlank(message = "El nombre del agente no puede estar vacío")
  private String name;

  @NotBlank(message = "La versión del agente no puede estar vacía")
  private String version;

  private String description;

  @NotBlank(message = "La URL de discovery del agente es obligatoria")
  private String discoveryUrl;

  private String repositoryUrl;

  // Getters y setters

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDiscoveryUrl() {
    return discoveryUrl;
  }

  public void setDiscoveryUrl(String discoveryUrl) {
    this.discoveryUrl = discoveryUrl;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }
}
