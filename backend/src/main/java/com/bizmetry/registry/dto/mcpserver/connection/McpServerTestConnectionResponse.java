package com.bizmetry.registry.dto.mcpserver.connection;

import java.util.List;

import com.bizmetry.registry.dto.mcpserver.McpToolDto;
import com.fasterxml.jackson.databind.JsonNode;

public class McpServerTestConnectionResponse {

  private boolean ok;
  private long latencyMs;
  private String resolvedUrl;
  private JsonNode metadata;
  private List<McpToolDto> tools;

  public McpServerTestConnectionResponse() {}

  public McpServerTestConnectionResponse(boolean ok, long latencyMs, String resolvedUrl, JsonNode metadata, List<McpToolDto> tools) {
    this.ok = ok;
    this.latencyMs = latencyMs;
    this.resolvedUrl = resolvedUrl;
    this.metadata = metadata;
    this.tools = tools;
  }

  public boolean isOk() { return ok; }
  public void setOk(boolean ok) { this.ok = ok; }

  public long getLatencyMs() { return latencyMs; }
  public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

  public String getResolvedUrl() { return resolvedUrl; }
  public void setResolvedUrl(String resolvedUrl) { this.resolvedUrl = resolvedUrl; }

  public JsonNode getMetadata() { return metadata; }
  public void setMetadata(JsonNode metadata) { this.metadata = metadata; }

  public List<McpToolDto> getTools() { return tools; }
  public void setTools(List<McpToolDto> tools) { this.tools = tools; }
}
