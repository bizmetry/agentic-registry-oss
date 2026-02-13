package com.bizmetry.registry.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizmetry.registry.dto.agent.MetadataDTO;
import com.bizmetry.registry.dto.mcpserver.McpServerCreateRequest;
import com.bizmetry.registry.dto.mcpserver.McpServerResponse;
import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.mcpserver.ToolInvokeRequest;
import com.bizmetry.registry.dto.mcpserver.ToolInvokeResponse;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition.OfficialMeta;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition.Remote;
import com.bizmetry.registry.dto.mcpserver.registry.McpRegistryDefinition.Server;
import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;
import com.bizmetry.registry.repo.AgentRepository;
import com.bizmetry.registry.repo.McpServerRepository;
import com.bizmetry.registry.web.errors.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class McpServerService {

  private static final String MCP_REGISTRY_SERVER_SCHEMA = "https://static.modelcontextprotocol.io/schemas/2025-09-29/server.schema.json";

  private static final String MCP_REGISTRY_OFFICIAL_META_KEY = "io.modelcontextprotocol.registry/official";

  private final McpServerRepository repo;
  private final ObjectMapper om;
  private final McpInvokeClient invokeClient;
  private final McpServerHealthService health;
  private final AgentRepository agentRepository;

  public McpServerService(
      McpServerRepository repo,
      ObjectMapper om,
      McpInvokeClient invokeClient,
      McpServerHealthService health,
      AgentRepository agentRepository) {
    this.repo = repo;
    this.om = om;
    this.invokeClient = invokeClient;
    this.health = health;
    this.agentRepository = agentRepository;
  }

  // ------------------------------------------------------------------
  // LIST (legacy) -> delega al nuevo
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public List<McpServerResponse> list(boolean includeTools) {
    return list(includeTools, null, "updatedTs", "desc");
  }

  // ------------------------------------------------------------------
  // LIST (new): search + sort (in-memory)
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public List<McpServerResponse> list(boolean includeTools, String q, String sortBy, String sortDir) {

    String term = normalizeSearch(q);

    List<McpServer> all = repo.findAll(); // ✅ sin cambiar repo: funciona ya

    // ✅ 1) FILTRO (search) por server.name o tools[].name
    List<McpServer> filtered;
    if (term == null) {
      filtered = all;
    } else {
      filtered = new ArrayList<>();
      for (McpServer s : all) {
        if (matchesServerOrTools(s, term)) {
          filtered.add(s);
        }
      }
    }

    // ✅ 2) SORT
    Comparator<McpServer> cmp = buildComparator(sortBy, sortDir);
    filtered.sort(cmp);

    // ✅ 3) MAP a response
    List<McpServerResponse> out = new ArrayList<>(filtered.size());
    for (McpServer s : filtered)
      out.add(toResponse(s, includeTools));
    return out;
  }

  private String normalizeSearch(String q) {
    if (q == null)
      return null;
    String t = q.trim().toLowerCase();
    return t.isEmpty() ? null : t;
  }

  private boolean matchesServerOrTools(McpServer s, String termLower) {
    // server name
    String name = s.getName();
    if (name != null && name.toLowerCase().contains(termLower))
      return true;

    // tools names (desde serverDoc persistido)
    List<McpToolDefinition> tools = extractTools(s.getServerDoc());
    for (McpToolDefinition t : tools) {
      if (t == null || t.getName() == null)
        continue;
      if (t.getName().toLowerCase().contains(termLower))
        return true;
    }
    return false;
  }

  private Comparator<McpServer> buildComparator(String sortBy, String sortDir) {
    String sb = (sortBy == null ? "updatedTs" : sortBy.trim());
    String sd = (sortDir == null ? "desc" : sortDir.trim());

    Comparator<McpServer> base;

    switch (sb) {
      case "name":
        base = Comparator.comparing(
            s -> safeLower(s.getName()),
            Comparator.nullsLast(String::compareTo));
        break;

      case "createdTs":
        base = Comparator.comparing(
            McpServer::getCreatedTs,
            Comparator.nullsLast(Comparator.naturalOrder()));
        break;

      case "updatedTs":
      default:
        base = Comparator.comparing(
            McpServer::getUpdatedTs,
            Comparator.nullsLast(Comparator.naturalOrder()));
        break;
    }

    boolean asc = "asc".equalsIgnoreCase(sd);
    return asc ? base : base.reversed();
  }

  private String safeLower(String s) {
    return s == null ? null : s.toLowerCase();
  }

  // ------------------------------------------------------------------
  // CRUD
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public McpServerResponse get(UUID serverId) {
    McpServer s = repo.findById(serverId)
        .orElseThrow(() -> new NotFoundException("MCP Server not found: " + serverId));
    return toResponse(s, true);
  }

  @Transactional
  public McpServerResponse create(McpServerCreateRequest req) {
    // 1. Validar si ya existe un servidor con el mismo nombre y versión

    if (!validateUniqueness(null, req.getName(), req.getVersion())) {
      throw new IllegalArgumentException("An MCP Server with the same name and version already exists.");
    }

    // Crear nuevo objeto McpServer
    McpServer s = new McpServer();
    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setVersion(req.getVersion());
    s.setDiscoveryUrl(req.getDiscoveryUrl());
    s.setServerDoc(buildDoc(null, req)); // Creamos el documento del servidor
    s.setStatus(McpServerStatus.ACTIVE);
    s.setRepositoryUrl(req.getRepositoryUrl());

    // Guardar el servidor en la base de datos
    s = repo.save(s);

    // Reemplazar el documento con el ID generado
    s.setServerDoc(buildDoc(s.getServerId(), req));
    s = repo.save(s);

    // Devolver la respuesta del servidor
    return toResponse(s, true);
  }

  @Transactional
  public McpServerResponse register(McpServerCreateRequest req) {
    // 1. Validar si ya existe un servidor con el mismo nombre y versión

    Optional<McpServer> s = repo.findByNameAndVersion(req.getName(), req.getVersion());

    if (s.isPresent()) {
      s.get().setDescription(req.getDescription());
      s.get().setDiscoveryUrl(req.getDiscoveryUrl());
      s.get().setRepositoryUrl(req.getRepositoryUrl());
      s.get().setServerDoc(buildDoc(s.get().getServerId(), req));
      repo.save(s.get());
      return toResponse(s.get(), true);

    } else {
      McpServer q = new McpServer();

      q.setName(req.getName());
      q.setDescription(req.getDescription());
      q.setVersion(req.getVersion());
      q.setDiscoveryUrl(req.getDiscoveryUrl());
      q.setServerDoc(buildDoc(null, req)); // Creamos el documento del servidor
      q.setStatus(McpServerStatus.ACTIVE);
      q.setRepositoryUrl(req.getRepositoryUrl());
      q.setServerDoc(buildDoc(q.getServerId(), req));
      repo.save(q);

      return toResponse(q, true);
    }
  }

  @Transactional
  public McpServerResponse update(UUID serverId, McpServerCreateRequest req) {
    McpServer s = repo.findById(serverId)
        .orElseThrow(() -> new NotFoundException("MCP Server not found: " + serverId));

    if (!validateUniqueness(serverId, req.getName(), req.getVersion())) {
      throw new IllegalArgumentException("An MCP Server with the same name and version already exists.");
    }

    // Actualizar los datos del servidor
    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setVersion(req.getVersion());
    s.setDiscoveryUrl(req.getDiscoveryUrl());
    s.setServerDoc(buildDoc(serverId, req)); // Actualiza el documento del servidor
    s.setRepositoryUrl(req.getRepositoryUrl());

    s = repo.save(s);

    // Ejecutar healthcheck y persistir estado/herramientas
    try {
      s = health.refreshNow(s); // REQUIRES_NEW (ver clase)
    } catch (Exception ignore) {
      // si deseas, loguear aquí, pero `health.refreshNow` ya loguea y persiste FAILED
      // si corresponde
    }

    return toResponse(s, true);
  }

  private Boolean validateUniqueness(UUID serverId, String serverName, String version) {
    List<McpServer> serverList = repo.findAll();

    Boolean found = false;
    for (McpServer mcpServer : serverList) {
      if (mcpServer.getName().toLowerCase().equals(serverName.toLowerCase()) &&
          mcpServer.getVersion().equals(version)
          && (serverId == null || (serverId != null &&
              !mcpServer.getServerId().equals(serverId)))) {
        found = true;
      }
    }

    return !found;
  }

  @Transactional
  public void delete(UUID serverId) {

    System.out.println("🗑️ Starting deletion of MCP Server: " + serverId);

    if (!repo.existsById(serverId)) {
      System.out.println("❌ MCP Server not found: " + serverId);
      throw new NotFoundException("MCP Server not found: " + serverId);
    }

    repo.deleteById(serverId);
    System.out.println("✅ MCP Server deleted from repository: " + serverId);

    int totalAgentsProcessed = 0;
    int totalAgentsUpdated = 0;

    for (Agent agent : agentRepository.findAll()) {

      totalAgentsProcessed++;
      System.out.println("🔎 Checking Agent: " + agent.getAgentId());

      MetadataDTO metadata = MetadataDTO.fromJsonNode(agent.getMetadata());

      int beforeCount = metadata.getTools().size();

      // 🔥 Remover tools asociadas al MCP Server eliminado
      boolean removed = metadata.getTools()
          .removeIf(tool -> Objects.equals(tool.getMcpServerId(), serverId));

      int afterCount = metadata.getTools().size();

      if (removed) {
        System.out.println("⚠️ Removed " + (beforeCount - afterCount) +
            " tools from Agent: " + agent.getAgentId());

        agent.setMetadata(metadata.convertToJsonNode());
        agentRepository.save(agent);

        totalAgentsUpdated++;
        System.out.println("💾 Agent metadata updated: " + agent.getAgentId());
      }
    }

    System.out.println("🏁 Deletion process completed.");
    System.out.println("📊 Agents processed: " + totalAgentsProcessed);
    System.out.println("📊 Agents updated: " + totalAgentsUpdated);
  }

  /// ------------------------------------------------------------------
  // OPENAI / OFFICIAL MCP REGISTRY DEFINITION (typed DTO)
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public McpRegistryDefinition buildOpenAiRegistryDefinition(UUID serverId) {

    McpServer s = repo.findById(serverId)
        .orElseThrow(() -> new NotFoundException("MCP Server not found: " + serverId));

    // ---- server ----
    Server server = new Server();
    server.setSchema(MCP_REGISTRY_SERVER_SCHEMA);
    server.setName(s.getName());
    server.setDescription(s.getDescription());
    server.setVersion(s.getVersion());

    // repository debe salir como {} en JSON (como tu ejemplo)
    // ✅ repository: exportar URL si está configurada; sino {}
    String repoUrl = s.getRepositoryUrl();
    if (repoUrl != null)
      repoUrl = repoUrl.trim();

    if (repoUrl != null && !repoUrl.isBlank()) {
      // -> serializa como {"url":"https://github.com/org/repo"}
      server.setRepository(Map.of("url", repoUrl));
    } else {
      // -> serializa como {}
      server.setRepository(Map.of());
    }

    // remotes: por ahora, usamos discoveryUrl como remote.url
    String remoteUrl = (s.getDiscoveryUrl() != null) ? s.getDiscoveryUrl() : "";
    server.setRemotes(List.of(new Remote("streamable-http", remoteUrl)));

    // ---- tools ----
    // reusamos las tools ya persistidas en serverDoc
    List<McpToolDefinition> toolDefs = extractTools(s.getServerDoc());
    if (toolDefs != null && !toolDefs.isEmpty()) {

      List<McpRegistryDefinition.Tool> tools = new ArrayList<>();

      for (McpToolDefinition t : toolDefs) {
        if (t == null || t.getName() == null || t.getName().isBlank())
          continue;

        McpRegistryDefinition.Tool tool = new McpRegistryDefinition.Tool();
        tool.setName(t.getName());
        tool.setDescription(t.getDescription());
        tool.setVersion(t.getVersion());
        tool.setArguments(t.getArguments()); // JsonNode (flexible)

        tools.add(tool);
      }

      if (!tools.isEmpty()) {
        server.setTools(tools);
      }
    }

    // ---- meta ----
    OfficialMeta official = new OfficialMeta();
    official.setStatus(mapToOfficialStatus(s.getStatus()));
    official.setPublishedAt(toInstantOrNull(s.getCreatedTs()));
    official.setUpdatedAt(toInstantOrNull(s.getUpdatedTs()));
    official.setIsLatest(Boolean.TRUE);

    McpRegistryDefinition out = new McpRegistryDefinition();
    out.setServer(server);
    out.setMeta(Map.of(MCP_REGISTRY_OFFICIAL_META_KEY, official));

    return out;
  }

  private String mapToOfficialStatus(McpServerStatus st) {
    if (st == null)
      return "inactive";
    switch (st) {
      case ACTIVE:
        return "active";
      default:
        return "inactive";
    }
  }

  /**
   * Si createdTs/updatedTs ya son Instant, esto no hace nada.
   * Si fueran null, devuelve null.
   *
   * Nota: si tu entidad usa otro tipo temporal (LocalDateTime/Date),
   * cambiá esta función por la conversión correcta y dejás el resto intacto.
   */
  private Instant toInstantOrNull(Object maybeInstant) {
    if (maybeInstant == null)
      return null;
    if (maybeInstant instanceof Instant)
      return (Instant) maybeInstant;
    // fallback: si no es Instant, preferimos null antes que meter algo incorrecto
    // (ajustalo si tu McpServer usa Date/LocalDateTime)
    return null;
  }

  // ------------------------------------------------------------------
  // DOC build + mapping
  // ------------------------------------------------------------------
  private JsonNode buildDoc(UUID serverId, McpServerCreateRequest req) {

    ObjectNode doc = om.createObjectNode();

    if (serverId != null) {
      doc.put("server_id", serverId.toString());
    }

    doc.put("name", req.getName());

    if (req.getDescription() != null) {
      doc.put("description", req.getDescription());
    }

    doc.put("version", req.getVersion());

    ObjectNode discovery = doc.putObject("discovery");
    discovery.put("url", req.getDiscoveryUrl());

    ArrayNode tools = doc.putArray("tools");

    if (req.getTools() != null) {
      for (McpToolDefinition t : req.getTools()) {

        ObjectNode tn = tools.addObject();
        tn.put("name", t.getName());

        if (t.getDescription() != null) {
          tn.put("description", t.getDescription());
        }

        if (t.getVersion() != null) {
          tn.put("version", t.getVersion());
        }

        if (t.getArguments() != null) {
          tn.set("arguments", t.getArguments());
        }

      }
    }

    // 🖨️ logs
    try {
      String prettyJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
      System.out.println("Resulting document:\n" + prettyJson);
    } catch (Exception e) {
      System.out.println("Resulting document (raw): " + doc.toString());
    }

    return doc;
  }

  private McpServerResponse toResponse(McpServer s, boolean includeTools) {
    McpServerResponse r = new McpServerResponse().builder()
        .serverId(s.getServerId())
        .name(s.getName())
        .description(s.getDescription())
        .version(s.getVersion())
        .discoveryUrl(s.getDiscoveryUrl())
        .createdTs(s.getCreatedTs())
        .updatedTs(s.getUpdatedTs())
        .status(s.getStatus())
        .repositoryUrl(s.getRepositoryUrl()).build();

    if (includeTools) {
      r.setTools(extractTools(s.getServerDoc()));
    }
    return r;
  }

  public List<McpToolDefinition> extractTools(JsonNode doc) {
    List<McpToolDefinition> out = new ArrayList<>();
    if (doc == null)
      return out;

    JsonNode tools = doc.get("tools");
    if (tools == null || !tools.isArray())
      return out;

    String docVersion = textOrNull(doc.get("version"));

    for (JsonNode t : tools) {
      String name = textOrNull(t.get("name"));
      if (name == null)
        continue;

      McpToolDefinition d = new McpToolDefinition();
      d.setName(name);
      d.setDescription(textOrNull(t.get("description")));

      String toolVersion = textOrNull(t.get("version"));
      d.setVersion((toolVersion != null) ? toolVersion : docVersion);

      JsonNode args = t.get("arguments");
      if (args != null && !args.isNull()) {
        d.setArguments(args);
      }

      out.add(d);
    }

    return out;
  }

  private String textOrNull(JsonNode n) {
    if (n == null || n.isNull())
      return null;
    String s = n.asText();
    if (s == null)
      return null;
    s = s.trim();
    return s.isEmpty() ? null : s;
  }

  @SuppressWarnings("unused")
  private String safeText(JsonNode node, String field) {
    JsonNode v = node.get(field);
    if (v == null || v.isNull())
      return null;
    String s = v.asText();
    return (s == null || s.isBlank()) ? null : s;
  }

  // ------------------------------------------------------------------
  // INVOKE TOOL
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public ToolInvokeResponse invokeTool(UUID serverId, String toolName, ToolInvokeRequest req) {
    long start = System.currentTimeMillis();

    McpServer s = repo.findById(serverId)
        .orElseThrow(() -> new NotFoundException("MCP Server not found: " + serverId));

    // ✅ validar tool existe (persistida)
    boolean exists = extractTools(s.getServerDoc()).stream()
        .anyMatch(t -> t.getName() != null && t.getName().equals(toolName));

    if (!exists) {
      exists = extractTools(s.getServerDoc()).stream()
          .anyMatch(t -> t.getName() != null && t.getName().equalsIgnoreCase(toolName));
    }

    if (!exists) {
      throw new NotFoundException("Tool not found on server: " + toolName);
    }

    boolean dryRun = req != null && Boolean.TRUE.equals(req.dryRun);

    ToolInvokeResponse resp = new ToolInvokeResponse();
    resp.serverId = serverId;
    resp.toolName = toolName;

    // ✅ normalizar args: Object -> Map<String,Object>
    @SuppressWarnings("unchecked")
    Map<String, Object> args = (req != null && req.args instanceof Map)
        ? (Map<String, Object>) req.args
        : Map.of();

    // ✅ normalizar timeout: Integer -> Long (ms)
    Long timeoutMs = (req != null && req.timeoutMs != null)
        ? req.timeoutMs.longValue()
        : 30000L;

    // ✅ bearer token opcional (si ya agregaste req.auth.bearerToken)
    String bearer = null;
    if (req != null && req.auth != null && req.auth.bearerToken != null) {
      bearer = req.auth.bearerToken.trim();
      if (bearer.toLowerCase().startsWith("bearer ")) {
        bearer = bearer.substring(7).trim();
      }
      if (bearer.isBlank())
        bearer = null;
    }

    if (dryRun) {
      resp.ok = true;

      ObjectNode node = om.createObjectNode();
      node.put("dryRun", true);
      node.put("toolName", toolName);
      node.put("discoveryUrl", s.getDiscoveryUrl());
      node.set("args", om.valueToTree(args));
      node.put("hasBearerToken", bearer != null);

      resp.result = node;
      resp.latencyMs = System.currentTimeMillis() - start;
      return resp;
    }

    try {
      // ✅ opción A (recomendada): agregar overload en McpInvokeClient que acepte
      // bearer
      Object result = invokeClient.invokeTool(s, toolName, args, timeoutMs, bearer);

      resp.ok = true;
      resp.result = result;
    } catch (Exception e) {
      resp.ok = false;
      resp.error = e.getMessage();
    }

    resp.latencyMs = System.currentTimeMillis() - start;
    return resp;
  }

}
