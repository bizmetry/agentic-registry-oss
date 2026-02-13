package com.bizmetry.registry.api;

import com.bizmetry.registry.dto.toolview.ToolViewResponse;
import com.bizmetry.registry.service.ToolViewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tools")
public class ToolsViewController {

  private final ToolViewService service;

  public ToolsViewController(ToolViewService service) {
    this.service = service;
  }

  /**
   * Read-only aggregated view of tools across all MCP Servers.
   * Pagination is simple: page is 1-based.
   */
  @GetMapping
  public List<ToolViewResponse> list(
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "size", defaultValue = "50") int size
  ) {
    return service.list(page, size);
  }
}
