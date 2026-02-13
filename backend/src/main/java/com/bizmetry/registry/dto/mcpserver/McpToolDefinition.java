package com.bizmetry.registry.dto.mcpserver;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class McpToolDefinition {

  @NotBlank
  @Size(max = 256)
  private String name;

  @Size(max = 4000)
  private String description;

  // La versión no siempre está presente
  @Size(max = 64)
  private String version;

  // Este es el argumento JSON Schema
  @NotNull
  private JsonNode arguments;

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

  public JsonNode getArguments() {
    return arguments;
  }

  public void setArguments(JsonNode arguments) {
    this.arguments = arguments;
  }

  // Builder Pattern
  public static McpToolDefinitionBuilder builder() {
    return new McpToolDefinitionBuilder();
  }

  public static class McpToolDefinitionBuilder {
    private String name;
    private String description;
    private String version;
    private JsonNode arguments;

    // Métodos del Builder
    public McpToolDefinitionBuilder name(String name) {
      this.name = name;
      return this;
    }

    public McpToolDefinitionBuilder description(String description) {
      this.description = description;
      return this;
    }

    public McpToolDefinitionBuilder version(String version) {
      this.version = version;
      return this;
    }

    public McpToolDefinitionBuilder arguments(JsonNode arguments) {
      this.arguments = arguments;
      return this;
    }

    public McpToolDefinition build() {
      McpToolDefinition toolDefinition = new McpToolDefinition();
      toolDefinition.setName(this.name);
      toolDefinition.setDescription(this.description);
      toolDefinition.setVersion(this.version);
      toolDefinition.setArguments(this.arguments);
      return toolDefinition;
    }
  }
}
