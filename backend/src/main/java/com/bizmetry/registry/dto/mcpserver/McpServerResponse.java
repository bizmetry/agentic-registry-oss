package com.bizmetry.registry.dto.mcpserver;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bizmetry.registry.model.McpServerStatus;

public class McpServerResponse {

  private UUID serverId;
  private String name;
  private String description;
  private String version;
  private String discoveryUrl;

  // ✅ NEW: GitHub repository URL (optional)
  private String repositoryUrl;

  // 🆕 estado del MCP Server
  private McpServerStatus status;

  private Instant createdTs;
  private Instant updatedTs;

  private List<McpToolDefinition> tools;

  // Getters y Setters

  public UUID getServerId() {
    return serverId;
  }

  public void setServerId(UUID serverId) {
    this.serverId = serverId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
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

  public McpServerStatus getStatus() {
    return status;
  }

  public void setStatus(McpServerStatus status) {
    this.status = status;
  }

  public Instant getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(Instant createdTs) {
    this.createdTs = createdTs;
  }

  public Instant getUpdatedTs() {
    return updatedTs;
  }

  public void setUpdatedTs(Instant updatedTs) {
    this.updatedTs = updatedTs;
  }

  public List<McpToolDefinition> getTools() {
    return tools;
  }

  public void setTools(List<McpToolDefinition> tools) {
    this.tools = tools;
  }

  // Builder Pattern
  public static McpServerResponseBuilder builder() {
    return new McpServerResponseBuilder();
  }

  public static class McpServerResponseBuilder {
    private UUID serverId;
    private String name;
    private String description;
    private String version;
    private String discoveryUrl;
    private String repositoryUrl;
    private McpServerStatus status;
    private Instant createdTs;
    private Instant updatedTs;
    private List<McpToolDefinition> tools;

    // Métodos del Builder
    public McpServerResponseBuilder serverId(UUID serverId) {
      this.serverId = serverId;
      return this;
    }

    public McpServerResponseBuilder name(String name) {
      this.name = name;
      return this;
    }

    public McpServerResponseBuilder description(String description) {
      this.description = description;
      return this;
    }

    public McpServerResponseBuilder version(String version) {
      this.version = version;
      return this;
    }

    public McpServerResponseBuilder discoveryUrl(String discoveryUrl) {
      this.discoveryUrl = discoveryUrl;
      return this;
    }

    public McpServerResponseBuilder repositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public McpServerResponseBuilder status(McpServerStatus status) {
      this.status = status;
      return this;
    }

    public McpServerResponseBuilder createdTs(Instant createdTs) {
      this.createdTs = createdTs;
      return this;
    }

    public McpServerResponseBuilder updatedTs(Instant updatedTs) {
      this.updatedTs = updatedTs;
      return this;
    }

    public McpServerResponseBuilder tools(List<McpToolDefinition> tools) {
      this.tools = tools;
      return this;
    }

    public McpServerResponse build() {
      McpServerResponse response = new McpServerResponse();
      response.setServerId(this.serverId);
      response.setName(this.name);
      response.setDescription(this.description);
      response.setVersion(this.version);
      response.setDiscoveryUrl(this.discoveryUrl);
      response.setRepositoryUrl(this.repositoryUrl);
      response.setStatus(this.status);
      response.setCreatedTs(this.createdTs);
      response.setUpdatedTs(this.updatedTs);
      response.setTools(this.tools);
      return response;
    }
  }
}
