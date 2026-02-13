package com.bizmetry.registry.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public class McpSession {

  private final String resolvedUrl;
  private final String sessionId; // puede ser null si el server no lo usa
  private final JsonNode initializeResult;

  public McpSession(String resolvedUrl, String sessionId, JsonNode initializeResult) {
    this.resolvedUrl = resolvedUrl;
    this.sessionId = sessionId;
    this.initializeResult = initializeResult;
  }

  public String getResolvedUrl() { return resolvedUrl; }
  public String getSessionId() { return sessionId; }
  public JsonNode getInitializeResult() { return initializeResult; }
}
