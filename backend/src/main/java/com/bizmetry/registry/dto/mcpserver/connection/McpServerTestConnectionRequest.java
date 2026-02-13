package com.bizmetry.registry.dto.mcpserver.connection;

import jakarta.validation.constraints.NotBlank;

public class McpServerTestConnectionRequest {
  @NotBlank
  private String discoveryUrl;

  public String getDiscoveryUrl() { return discoveryUrl; }
  public void setDiscoveryUrl(String discoveryUrl) { this.discoveryUrl = discoveryUrl; }
}
