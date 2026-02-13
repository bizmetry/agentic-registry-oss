package com.bizmetry.registry.mcp;

public class McpClientException extends RuntimeException {
  public McpClientException(String message) { super(message); }
  public McpClientException(String message, Throwable cause) { super(message, cause); }
}
