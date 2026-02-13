package com.bizmetry.registry.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.jobs.McpToolNormalizer;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;
import com.bizmetry.registry.repo.McpServerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class McpServerHealthService {

  private static final Logger log = LoggerFactory.getLogger(McpServerHealthService.class);

  private final McpServerRepository repo;
  private final McpInvokeClient mcp;
  private final ObjectMapper om;

  public McpServerHealthService(McpServerRepository repo, McpInvokeClient mcp, ObjectMapper om) {
    this.repo = repo;
    this.mcp = mcp;
    this.om = om;
  }

  /**
   * Corre un healthcheck inmediato y persiste status/tools.
   * REQUIRES_NEW para no mezclar con la TX del update (y no sostener locks mientras haces HTTP).
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public McpServer refreshNow(UUID serverId) {
    McpServer s = repo.findById(serverId)
        .orElseThrow(() -> new RuntimeException("MCP Server not found: " + serverId));

    return refreshNow(s);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public McpServer refreshNow(McpServer s) {
    UUID id = s.getServerId();
    String url = s.getDiscoveryUrl();

    log.info("[MCP][HEALTH] start serverId={} name='{}' url={}", id, s.getName(), url);

    ObjectNode doc = ensureDocObject(s.getServerDoc());
    Instant now = Instant.now();

    try {
      // (Opcional) initialize: si tu server realmente lo necesita.
      // Si no, sacalo para no romper servidores que no soportan initialize por HTTP.
      // mcp.call(s, "initialize", Map.of("client", Map.of("name","bizmetry-registry","version","1.0")), 10000L);

      // tools/list (lo más práctico como health real)
      Object result = mcp.call(s, "tools/list", Map.of(), 15000L);

      List<McpToolDefinition> tools = McpToolNormalizer.normalize(result, om);

      // persist tools dentro del doc
      ArrayNode toolsArr = doc.putArray("tools");
      for (McpToolDefinition t : tools) {
        ObjectNode tn = toolsArr.addObject();
        tn.put("name", t.getName());
        if (t.getDescription() != null) tn.put("description", t.getDescription());
        if (t.getVersion() != null) tn.put("version", t.getVersion());
        if (t.getArguments() != null) tn.set("arguments", t.getArguments());
      }

      doc.put("last_tools_refresh_ts", now.toString());
      doc.put("last_healthcheck_ts", now.toString());
      doc.remove("last_healthcheck_error");

      s.setServerDoc(doc);
      s.setStatus(McpServerStatus.ACTIVE);

      McpServer saved = repo.save(s);

      log.info("[MCP][HEALTH] OK serverId={} tools={} status={}",
          id, tools.size(), saved.getStatus());

      return saved;

    } catch (Exception e) {
      String msg = safeMsg(e);
      doc.put("last_healthcheck_ts", now.toString());
      doc.put("last_healthcheck_error", msg);

      s.setServerDoc(doc);
      s.setStatus(McpServerStatus.FAILED);

      McpServer saved = repo.save(s);

      log.warn("[MCP][HEALTH] FAILED serverId={} status={} error={}", id, saved.getStatus(), msg);

      return saved;
    }
  }

  private ObjectNode ensureDocObject(JsonNode doc) {
    if (doc != null && doc.isObject()) return (ObjectNode) doc;
    return om.createObjectNode();
  }

  private String safeMsg(Exception e) {
    String m = e.getMessage();
    return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
  }
}
