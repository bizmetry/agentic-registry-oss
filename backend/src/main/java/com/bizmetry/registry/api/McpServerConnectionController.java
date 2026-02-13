package com.bizmetry.registry.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionRequest;
import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionResponse;
import com.bizmetry.registry.service.McpConnectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/api/registry/mcp-servers")
public class McpServerConnectionController {

  private final McpConnectionService service;

  public McpServerConnectionController(McpConnectionService service) {
    this.service = service;
  }

  @PostMapping("/test-connection")
  public McpServerTestConnectionResponse testConnection(@Valid @RequestBody McpServerTestConnectionRequest req) {
    try {
      return service.testConnection(req.getDiscoveryUrl());
    } catch (Exception e) {
        e.printStackTrace();;
      // requisito: si falla la conexion => 500
      throw new McpServerConnectionException("MCP connection test failed: " + e.getMessage(), e);
    }
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  static class McpServerConnectionException extends RuntimeException {
    McpServerConnectionException(String msg, Throwable cause) { super(msg, cause); }
  }
}
