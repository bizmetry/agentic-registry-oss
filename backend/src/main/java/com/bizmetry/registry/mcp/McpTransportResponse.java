package com.bizmetry.registry.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Low-level transport response from an MCP HTTP call.
 */
public class McpTransportResponse {

  private final int statusCode;
  private final String body;
  private final String sessionIdHeader;
  private final JsonNode json;

  public McpTransportResponse(
      int statusCode,
      String body,
      String sessionIdHeader,
      JsonNode json
  ) {
    this.statusCode = statusCode;
    this.body = body;
    this.sessionIdHeader = sessionIdHeader;
    this.json = json;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getBody() {
    return body;
  }

  public String getSessionIdHeader() {
    return sessionIdHeader;
  }

  public JsonNode getJson() {
    return json;
  }
}
