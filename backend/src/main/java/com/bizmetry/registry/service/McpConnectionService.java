package com.bizmetry.registry.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bizmetry.registry.dto.mcpserver.McpToolDto;
import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionResponse;
import com.bizmetry.registry.mcp.McpClient;
import com.bizmetry.registry.mcp.McpSession;
import com.bizmetry.registry.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class McpConnectionService {

  private final McpClient mcpClient;

  public McpConnectionService(McpClient mcpClient) {
    this.mcpClient = mcpClient;
  }

  public McpServerTestConnectionResponse testConnection(String discoveryUrl) {
    long start = System.nanoTime();

    McpSession session = mcpClient.initialize(discoveryUrl);

    // metadata raw JSON from initialize
    JsonNode metadata = session.getInitializeResult();

    List<McpTool> tools = mcpClient.listTools(session);

    List<McpToolDto> toolDtos = tools.stream()
        .map(t -> new McpToolDto(t.getName(), t.getDescription(), t.getArguments()))
        .toList();

    long latencyMs = (System.nanoTime() - start) / 1_000_000;

    return new McpServerTestConnectionResponse(
        true,
        latencyMs,
        session.getResolvedUrl(),
        metadata,
        toolDtos
    );
  }
}
