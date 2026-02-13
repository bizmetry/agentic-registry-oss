package com.bizmetry.registry.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizmetry.registry.dto.mcpserver.McpServerCreateRequest;
import com.bizmetry.registry.dto.mcpserver.McpServerResponse;
import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.mcpserver.McpToolDto;
import com.bizmetry.registry.dto.mcpserver.ToolInvokeRequest;
import com.bizmetry.registry.dto.mcpserver.ToolInvokeResponse;
import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionResponse;
import com.bizmetry.registry.dto.mcpserver.register.McpServerRegisterRequest;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.service.McpConnectionService;
import com.bizmetry.registry.service.McpServerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/api/registry/mcp-servers")
public class McpServersController {

  private final McpServerService service;
  private final McpConnectionService connectionService;
 

  public McpServersController(McpServerService service, McpConnectionService connectionService) {
    this.service = service;
    this.connectionService = connectionService;
  
  }

  @GetMapping
  public List<McpServerResponse> list(
      @RequestParam(name = "includeTools", defaultValue = "true") boolean includeTools,
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "sortBy", defaultValue = "updatedTs") String sortBy,
      @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
    return service.list(includeTools, q, sortBy, sortDir);
  }

  @GetMapping("/{serverId}")
  public McpServerResponse get(@PathVariable("serverId") UUID serverId) {
    return service.get(serverId);
  }

  @PostMapping
  public McpServerResponse create(@Valid @RequestBody McpServerCreateRequest req) {
    return service.create(req);
  }

  @PutMapping("/{serverId}")
  public McpServerResponse update(
      @PathVariable("serverId") UUID serverId,
      @Valid @RequestBody McpServerCreateRequest req) {
    return service.update(serverId, req);
  }

  @DeleteMapping("/{serverId}")
  public void delete(@PathVariable("serverId") UUID serverId) {
    service.delete(serverId);
  }

  @PostMapping("/{serverId}/tools/{toolName}/invoke")
  public ToolInvokeResponse invoke(
      @PathVariable("serverId") UUID serverId,
      @PathVariable("toolName") String toolName,
      @Valid @RequestBody ToolInvokeRequest req) {
    return service.invokeTool(serverId, toolName, req);
  }

  @GetMapping("/{serverId}/definition")
  public McpRegistryDefinition definition(@PathVariable UUID serverId) {
    return service.buildOpenAiRegistryDefinition(serverId);
  }

  @PostMapping("/register")
  public McpServerResponse registerAgent(@RequestBody McpServerRegisterRequest req) {

    try {
      // 1) Validar la URL de discovery del agente usando McpConnectionService
      McpServerTestConnectionResponse connectionResponse = connectionService.testConnection(req.getDiscoveryUrl());

      if (!connectionResponse.isOk()) {
        throw new IllegalArgumentException("La URL de discovery no es válida o no responde.");
      }

      // 3) Persistir el MCP server en la base de datos

      List<McpToolDefinition> tools = new ArrayList<McpToolDefinition>();
      for (McpToolDto tool : connectionResponse.getTools()) {

        McpToolDefinition tooldef = new McpToolDefinition().builder()
            .arguments(tool.getArguments())
            .description(tool.getDescription())
            .name(tool.getName())
            .build();

        tools.add(tooldef);
      }
 
      McpServerCreateRequest thisRequest = new McpServerCreateRequest().builder()
          .description(req.getDescription())
          .discoveryUrl(req.getDiscoveryUrl())
          .name(req.getName())
          .repositoryUrl(req.getRepositoryUrl())
          .version(req.getVersion())
          .tools(tools)
          .build();
      McpServerResponse response = service.register(thisRequest);

      // 4) Devolver el ID del agente creado
      return response;

    } catch (Exception e) {
      e.printStackTrace();
      ;
      // Manejo de errores
      throw new RuntimeException("Error while registering the MCP Server" + e.getMessage(), e);
    }
  }
}
