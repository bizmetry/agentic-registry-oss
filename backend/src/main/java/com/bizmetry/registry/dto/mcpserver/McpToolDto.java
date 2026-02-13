package com.bizmetry.registry.dto.mcpserver;

import com.fasterxml.jackson.databind.JsonNode;

public class McpToolDto {
  private String name;
  private String description;
  private JsonNode arguments;

  public McpToolDto() {}

  public McpToolDto(String name, String description, JsonNode arguments) {
    this.name = name;
    this.description = description;
    this.arguments = arguments;
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public JsonNode getArguments() { return arguments; }
  public void setArguments(JsonNode arguments) { this.arguments = arguments; }
}

