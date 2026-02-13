package com.bizmetry.registry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizmetry.registry.dto.mcpserver.McpServerCreateRequest;
import com.bizmetry.registry.dto.mcpserver.McpServerResponse;
import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.mcpserver.importing.McpServerImportRequest;
import com.bizmetry.registry.dto.mcpserver.importing.McpServerImportResponse;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;
import com.bizmetry.registry.repo.McpServerRepository;
import com.bizmetry.registry.web.errors.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class McpServerImportService {

  private final ObjectMapper om;
  private final McpServerRepository repo;
  private final McpServerService serverService; // reutilizamos create/update/toResponse

  public McpServerImportService(ObjectMapper om, McpServerRepository repo, McpServerService serverService) {
    this.om = om;
    this.repo = repo;
    this.serverService = serverService;
  }

  @Transactional
  public McpServerImportResponse importFromExport(McpServerImportRequest req) {
    McpServerImportResponse out = new McpServerImportResponse();
    out.ok = false;

    try {
      boolean dryRun = req != null && Boolean.TRUE.equals(req.dryRun);
      boolean upsert = req == null || req.upsert == null || Boolean.TRUE.equals(req.upsert);

      // payload -> JsonNode
      JsonNode node = om.valueToTree(req == null ? null : req.payload);

      // puede venir 1 server o lista de servers
      List<McpRegistryDefinition> defs = parseAsList(node);

      int created = 0;
      int updated = 0;
      List<McpServerResponse> imported = new ArrayList<>();

      for (McpRegistryDefinition def : defs) {
        McpServerCreateRequest cr = mapRegistryToCreate(def);

        if (dryRun) {
          // no persistimos, solo devolvemos preview como response “fake”
          McpServerResponse preview = new McpServerResponse();
          preview.setName(cr.getName());
          preview.setDescription(cr.getDescription());
          preview.setVersion(cr.getVersion());
          preview.setDiscoveryUrl(cr.getDiscoveryUrl());
          preview.setRepositoryUrl(cr.getRepositoryUrl());
          preview.setStatus( McpServerStatus.ACTIVE);
          preview.setTools(cr.getTools());
         
          imported.add(preview);
          continue;
        }

        Optional<McpServer> existing = repo.findByNameAndVersion(cr.getName(), cr.getVersion());

        if (existing.isPresent()) {
          if (!upsert) {
            // si no permitís upsert, lo salteás o tirás error. Acá tiro error:
            throw new IllegalArgumentException("Server already exists: " + cr.getName() + " " + cr.getVersion());
          }
          McpServerResponse r = serverService.update(existing.get().getServerId(), cr);
          imported.add(r);
          updated++;
        } else {
          McpServerResponse r = serverService.create(cr);
          imported.add(r);
          created++;
        }
      }

      out.ok = true;
      out.created = created;
      out.updated = updated;
      out.servers = imported;
      return out;

    } catch (Exception e) {
      out.error = e.getMessage();
      return out;
    }
  }

  private List<McpRegistryDefinition> parseAsList(JsonNode node) {
    if (node == null || node.isNull()) {
      throw new IllegalArgumentException("payload is required");
    }

    List<McpRegistryDefinition> out = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode it : node) {
        out.add(om.convertValue(it, McpRegistryDefinition.class));
      }
      return out;
    }

    // single object
    out.add(om.convertValue(node, McpRegistryDefinition.class));
    return out;
  }

  private McpServerCreateRequest mapRegistryToCreate(McpRegistryDefinition def) {
    if (def == null || def.getServer() == null) {
      throw new IllegalArgumentException("Invalid registry JSON: missing server");
    }

    McpRegistryDefinition.Server s = def.getServer();

    McpServerCreateRequest cr = new McpServerCreateRequest();
    cr.setName(s.getName());
    cr.setDescription(s.getDescription());
    cr.setVersion(s.getVersion()); 

    // discoveryUrl desde remotes[0].url (como tu export)
    String discoveryUrl = null;
    if (s.getRemotes() != null && !s.getRemotes().isEmpty() && s.getRemotes().get(0) != null) {
      discoveryUrl = s.getRemotes().get(0).getUrl();
    }
    if (discoveryUrl == null || discoveryUrl.isBlank()) {
      throw new IllegalArgumentException("Invalid registry JSON: missing server.remotes[0].url");
    }

    cr.setDiscoveryUrl(discoveryUrl);

    // repositoryUrl: repository.url (si existe)
    String repoUrl = null;
    Map<String, Object> repoMap = s.getRepository();
    if (repoMap != null) {
      Object url = repoMap.get("url");
      if (url != null) repoUrl = String.valueOf(url);
    }

    
    cr.setRepositoryUrl(repoUrl != null && !repoUrl.trim().isBlank() ? repoUrl.trim() : null);

    // tools
    if (s.getTools() != null && !s.getTools().isEmpty()) {
      cr.setTools(new ArrayList <> ());
      
      for (McpRegistryDefinition.Tool t : s.getTools()) {
        McpToolDefinition td = new McpToolDefinition();
        td.setName(t.getName());
        td.setDescription(t.getDescription());
        td.setVersion(t.getVersion());
        td.setArguments(t.getArguments());
        cr.getTools().add(td);
      }
    }

    // mínima validación
    if (cr.getName() == null || cr.getName().isBlank()) throw new IllegalArgumentException("Missing server.name");
    if (cr.getVersion() == null || cr.getVersion().isBlank()) throw new IllegalArgumentException("Missing server.version");

    return cr;
  }
}
