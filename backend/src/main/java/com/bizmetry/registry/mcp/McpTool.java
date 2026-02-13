package com.bizmetry.registry.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public class McpTool {
  private final String name;
  private final String description;
  private final JsonNode arguments; // inputSchema / parameters schema

  public McpTool(String name, String description, JsonNode arguments) {
    this.name = name;
    this.description = description;
    this.arguments = arguments;
  }

  public String getName() { return name; }
  public String getDescription() { return description; }
  public JsonNode getArguments() { return arguments; }
}
