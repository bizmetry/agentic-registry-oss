package com.bizmetry.registry.service;

import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.toolview.ToolViewResponse;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.repo.McpServerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ToolViewService {

  private final McpServerRepository serverRepo;
  private final McpServerService mcpServerService;

  public ToolViewService(McpServerRepository serverRepo, McpServerService mcpServerService) {
    this.serverRepo = serverRepo;
    this.mcpServerService = mcpServerService;
  }

  @Transactional(readOnly = true)
  public List<ToolViewResponse> list(int page, int size) {
    if (page < 1) page = 1;
    if (size < 1) size = 50;
    if (size > 500) size = 500;

    List<McpServer> servers = serverRepo.findAll();
    List<ToolViewResponse> all = new ArrayList<>();

    for (McpServer s : servers) {
      List<McpToolDefinition> tools = mcpServerService.extractTools(s.getServerDoc());
      for (McpToolDefinition t : tools) {
        ToolViewResponse r = new ToolViewResponse();
        r.serverId = s.getServerId();
        r.serverName = s.getName();
        r.serverVersion = s.getVersion();
        r.discoveryUrl = s.getDiscoveryUrl();
        r.name = t.getName();
        r.description = t.getDescription();
        r.version = t.getVersion();
        all.add(r);
      }
    }

    all.sort(Comparator.comparing((ToolViewResponse x) -> (x.name == null ? "" : x.name))
        .thenComparing(x -> (x.version == null ? "" : x.version)));

    int from = (page - 1) * size;
    if (from >= all.size()) return List.of();
    int to = Math.min(all.size(), from + size);
    return all.subList(from, to);
  }
}
