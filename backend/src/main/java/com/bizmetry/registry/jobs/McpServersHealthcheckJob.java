package com.bizmetry.registry.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;
import com.bizmetry.registry.repo.McpServerRepository;
import com.bizmetry.registry.service.McpInvokeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class McpServersHealthcheckJob {

  private static final Logger log = LoggerFactory.getLogger(McpServersHealthcheckJob.class);

  private final McpServerRepository repo;
  private final McpInvokeClient mcp;
  private final ObjectMapper om;

  public McpServersHealthcheckJob(McpServerRepository repo, McpInvokeClient mcp, ObjectMapper om) {
    this.repo = repo;
    this.mcp = mcp;
    this.om = om;
  }

  /**
   * ✅ Corre:
   * - initialDelay: para que no dispare instantáneo mientras la app todavía levanta
   * - fixedDelay: cada X ms después de terminar la corrida anterior
   */
  @Scheduled(
      initialDelayString = "${bizmetry.mcp.healthcheck.initialDelayMs:2000}",
      fixedDelayString = "${bizmetry.mcp.healthcheck.fixedDelayMs:60000}"
  )
  @Transactional
  public void run() {
    long jobStart = System.currentTimeMillis();
    log.info("🔁 MCP healthcheck job started at {}", Instant.now());

    List<McpServer> servers = repo.findByStatusNot(McpServerStatus.DISABLED);

    if (servers.isEmpty()) {
      log.info("ℹ️ No MCP servers to check (excluding DISABLED). Done.");
      return;
    }

    log.info("📡 Checking {} MCP servers...", servers.size());

    int ok = 0;
    int failed = 0;

    for (McpServer s : servers) {
      long oneStart = System.currentTimeMillis();
      String sid = String.valueOf(s.getServerId());

      log.info("➡️ Checking [{}] name='{}' url='{}' currentStatus={}",
          sid, s.getName(), s.getDiscoveryUrl(), s.getStatus()
      );

      try {
        refreshOne(s);

        ok++;
        log.info("✅ OK [{}] in {} ms (status now={})",
            sid,
            (System.currentTimeMillis() - oneStart),
            s.getStatus()
        );

      } catch (Exception e) {
        failed++;
        log.warn("❌ FAILED [{}] in {} ms -> {}",
            sid,
            (System.currentTimeMillis() - oneStart),
            safeMsg(e)
        );
        // refreshOne ya setea FAILED y persiste doc, pero por si acaso:
      }
    }

    log.info("🏁 MCP healthcheck job finished in {} ms | ok={} failed={}",
        (System.currentTimeMillis() - jobStart),
        ok,
        failed
    );
  }

  private void refreshOne(McpServer s) {
    try {
      // 1) initialize (health)
      log.debug("   ↪ initialize() on {}", s.getDiscoveryUrl());

      mcp.call(s, "initialize",
    Map.of(
        "protocolVersion", "2025-06-18",          // o "2025-03-26" si querés ir a lo seguro
        "capabilities", Map.of(),                 // mínimo objeto vacío
        "clientInfo", Map.of(
            "name", "bizmetry-registry",
            "version", "1.0"
        )
    ),
    10000L
);
      // 2) tools/list
      log.debug("   ↪ tools/list on {}", s.getDiscoveryUrl());

      Object result = mcp.call(s, "tools/list", Map.of(), 15000L);

      // 3) persist tools + status ACTIVE + timestamps (en doc)
      ObjectNode doc = ensureDocObject(s.getServerDoc());

      List<McpToolDefinition> tools = McpToolNormalizer.normalize(result, om);

      ArrayNode toolsArr = doc.putArray("tools");
      for (McpToolDefinition t : tools) {
        ObjectNode tn = toolsArr.addObject();
        tn.put("name", t.getName());
        if (t.getDescription() != null) tn.put("description", t.getDescription());
        if (t.getVersion() != null) tn.put("version", t.getVersion());
        if (t.getArguments() != null) tn.set("arguments", t.getArguments());
      }

      Instant now = Instant.now();
      doc.put("last_tools_refresh_ts", now.toString());
      doc.put("last_healthcheck_ts", now.toString());
      doc.remove("last_healthcheck_error");

      s.setServerDoc(doc);
      s.setStatus(McpServerStatus.ACTIVE);

      repo.save(s);

      log.info("   🔄 Refreshed tools for '{}' (count={})", s.getName(), tools.size());

    } catch (Exception e) {

      ObjectNode doc = ensureDocObject(s.getServerDoc());
      Instant now = Instant.now();
      doc.put("last_healthcheck_ts", now.toString());
      doc.put("last_healthcheck_error", safeMsg(e));

      s.setServerDoc(doc);
      s.setStatus(McpServerStatus.FAILED);

      repo.save(s);

      // Re-lanzamos para que el loop de run() lo cuente como failed y loguee también
      throw e;
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

