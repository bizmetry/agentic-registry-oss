package com.bizmetry.registry.dto.mcpserver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class McpServerCreateRequest {

  @NotBlank
  @Size(max = 256)
  private String name;

  @Size(max = 4000)
  private String description;

  @NotBlank
  @Size(max = 64)
  private String version;

  @NotBlank
  @Size(max = 1024)
  private String discoveryUrl;

  // ✅ NEW: GitHub repository URL (optional)
  @Size(max = 512)
  @Pattern(
      regexp = "^$|https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?$",
      message = "repositoryUrl must be a valid GitHub repo URL like https://github.com/org/repo"
  )
  private String repositoryUrl;

  private List<McpToolDefinition> tools;

  // Getters y Setters
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

  public List<McpToolDefinition> getTools() {
    return tools;
  }

  public void setTools(List<McpToolDefinition> tools) {
    this.tools = tools;
  }

  // Builder Pattern
  public static McpServerCreateRequestBuilder builder() {
    return new McpServerCreateRequestBuilder();
  }

  public static class McpServerCreateRequestBuilder {
    private String name;
    private String description;
    private String version;
    private String discoveryUrl;
    private String repositoryUrl;
    private List<McpToolDefinition> tools;

    // Métodos del Builder
    public McpServerCreateRequestBuilder name(String name) {
      this.name = name;
      return this;
    }

    public McpServerCreateRequestBuilder description(String description) {
      this.description = description;
      return this;
    }

    public McpServerCreateRequestBuilder version(String version) {
      this.version = version;
      return this;
    }

    public McpServerCreateRequestBuilder discoveryUrl(String discoveryUrl) {
      this.discoveryUrl = discoveryUrl;
      return this;
    }

    public McpServerCreateRequestBuilder repositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public McpServerCreateRequestBuilder tools(List<McpToolDefinition> tools) {
      this.tools = tools;
      return this;
    }

    public McpServerCreateRequest build() {
      McpServerCreateRequest request = new McpServerCreateRequest();
      request.setName(this.name);
      request.setDescription(this.description);
      request.setVersion(this.version);
      request.setDiscoveryUrl(this.discoveryUrl);
      request.setRepositoryUrl(this.repositoryUrl);
      request.setTools(this.tools);
      return request;
    }
  }
}
