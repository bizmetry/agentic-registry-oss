package com.bizmetry.registry.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bizmetry.registry.dto.mcpserver.importing.McpServerImportRequest;
import com.bizmetry.registry.dto.mcpserver.importing.McpServerImportResponse;
import com.bizmetry.registry.service.McpServerImportService;

@RestController
@RequestMapping("/v1/api/registry/mcp-servers")
public class McpServerImportController {

  private final McpServerImportService importService;

  public McpServerImportController(McpServerImportService importService) {
    this.importService = importService;
  }

  @PostMapping("/import")
  public ResponseEntity<McpServerImportResponse> importServers(@RequestBody McpServerImportRequest req) {
    McpServerImportResponse resp = importService.importFromExport(req);
    return resp.ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
  }
}
