package com.bizmetry.registry.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bizmetry.registry.utils.ExceptionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpMcpClient implements McpClient {

  private final ObjectMapper mapper;
  private final HttpClient http;

  public HttpMcpClient(ObjectMapper mapper) {
    this.mapper = mapper;
    this.http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();
  }

  /**
   * ✅ Ahora discoveryUrl viene como HTTP/HTTPS (no mcp://)
   * Ej: https://mcp.exa.ai/mcp
   */
  public McpSession initialize(String discoveryUrl) {

    if (discoveryUrl == null || discoveryUrl.isBlank()) {
      throw new McpClientException("discoveryUrl is required");
    }

    String resolved = normalizeHttpUrl(discoveryUrl);

    if (!isHttpScheme(resolved)) {
      throw new McpClientException("discoveryUrl must start with http:// or https:// (received: " + discoveryUrl + ")");
    }

    Exception last = null;

    // ✅ Si querés mantener fallback automático por si alguien eligió mal scheme:
    // probamos primero el que vino, y luego el alternativo.
    List<String> candidates = new ArrayList<>();
    candidates.add(resolved);

    String alternate = alternateScheme(resolved);
    if (alternate != null && !alternate.equalsIgnoreCase(resolved)) {
      candidates.add(alternate);
    }

    System.out.println("Discovery URL candidates: " + candidates);

    for (String url : candidates) {
      try {
        JsonNode initPayload = McpJsonRpc.initialize(mapper);
        McpTransportResponse resp = postJson(url, initPayload, Optional.empty());

        JsonNode root = resp.getJson();
        JsonNode result = root.path("result");

        if (result.isMissingNode() || result.isNull()) {
          JsonNode err = root.path("error");
          String details = (!err.isMissingNode() && !err.isNull())
              ? err.toString()
              : resp.getBody();

          throw new McpClientException("MCP initialize failed: " + details);
        }

        String sessionId = extractSessionId(resp, result);
        return new McpSession(url, sessionId, result);

      } catch (Exception e) {
        last = e;
        System.out.println("CONNECT FAILED to " + url + " -> " + ExceptionUtils.friendlyMessage(e));
        e.printStackTrace();
      }
    }

    String msg = "Unable to connect via MCP. Tried: " + candidates
        + ". Last error: " + ExceptionUtils.friendlyMessage(last);

    throw new McpClientException(msg, last);
  }

  @Override
  public List<McpTool> listTools(McpSession session) {
    if (session == null) throw new McpClientException("session is required");

    try {
      JsonNode payload = McpJsonRpc.toolsList(mapper);
      McpTransportResponse resp = postJson(
          session.getResolvedUrl(),
          payload,
          Optional.ofNullable(session.getSessionId())
      );

      JsonNode root = resp.getJson();
      JsonNode result = root.path("result");
      if (result.isMissingNode() || result.isNull()) {
        JsonNode err = root.path("error");
        throw new McpClientException("MCP tools/list failed: " + (err.isMissingNode() ? resp.getBody() : err.toString()));
      }

      return normalizeTools(result);

    } catch (Exception e) {
      throw new McpClientException("Unable to list tools: " + ExceptionUtils.friendlyMessage(e), e);
    }
  }

  // ----------------------------
  // Transport
  // ----------------------------

  private McpTransportResponse postJson(String url, JsonNode payload, Optional<String> sessionIdOpt) throws Exception {

    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(12))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        // ✅ requerido por exa.ai y otros MCP servers
        .header("Accept", "application/json, text/event-stream")
        .header("Cache-Control", "no-cache");

    sessionIdOpt.ifPresent(sid -> b.header("mcp-session-id", sid));

    HttpRequest req = b
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new McpClientException("HTTP " + resp.statusCode() + " calling " + url + " -> " + truncate(resp.body()));
    }

    String sidHeader = resp.headers().firstValue("mcp-session-id").orElse(null);

    // ✅ Detectar JSON vs SSE
    String contentType = resp.headers().firstValue("content-type").orElse("");
    String body = resp.body() == null ? "" : resp.body();

    JsonNode json;
    if (isEventStream(contentType, body)) {
      String jsonText = extractJsonFromSse(body);
      if (jsonText == null || jsonText.isBlank()) {
        throw new McpClientException("SSE response received but no JSON payload found. url=" + url + " -> " + truncate(body));
      }
      json = mapper.readTree(jsonText);
    } else {
      json = mapper.readTree(body);
    }

    return new McpTransportResponse(resp.statusCode(), body, sidHeader, json);
  }

  private static boolean isEventStream(String contentType, String body) {
    if (contentType != null && contentType.toLowerCase().contains("text/event-stream")) return true;
    if (body == null) return false;
    String t = body.trim();
    return t.startsWith("event:") || t.startsWith("data:") || t.startsWith("id:");
  }

  /**
   * Extrae JSON desde una respuesta SSE.
   */
  private String extractJsonFromSse(String sseBody) {
    if (sseBody == null || sseBody.isBlank()) return null;

    String[] lines = sseBody.split("\\r?\\n");
    StringBuilder dataBuf = new StringBuilder();
    String lastGoodJson = null;

    for (String line : lines) {
      if (line.startsWith("data:")) {
        String chunk = line.substring("data:".length()).trim();
        if (!chunk.isEmpty()) {
          dataBuf.append(chunk).append("\n");
        }
        continue;
      }

      if (line.trim().isEmpty() && dataBuf.length() > 0) {
        String candidate = dataBuf.toString().trim();
        dataBuf.setLength(0);

        if (candidate.equals("[DONE]")) continue;

        try {
          mapper.readTree(candidate);
          lastGoodJson = candidate;
        } catch (Exception ignore) {}
      }
    }

    if (lastGoodJson == null && dataBuf.length() > 0) {
      String candidate = dataBuf.toString().trim();
      try {
        mapper.readTree(candidate);
        lastGoodJson = candidate;
      } catch (Exception ignore) {}
    }

    return lastGoodJson;
  }

  private String truncate(String s) {
    if (s == null) return null;
    return s.length() > 500 ? s.substring(0, 500) + "..." : s;
  }

  // ----------------------------
  // Helpers
  // ----------------------------

  private String extractSessionId(McpTransportResponse initResp, JsonNode initResult) {
    if (initResp.getSessionIdHeader() != null && !initResp.getSessionIdHeader().isBlank()) {
      return initResp.getSessionIdHeader();
    }

    String[] candidates = {
        "sessionId", "session_id",
        "session.id",
        "transport.sessionId", "transport.session_id"
    };

    for (String c : candidates) {
      JsonNode v = getPath(initResult, c);
      if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
    }
    return null;
  }

  private JsonNode getPath(JsonNode node, String dotted) {
    String[] parts = dotted.split("\\.");
    JsonNode cur = node;
    for (String p : parts) {
      if (cur == null) return null;
      cur = cur.path(p);
      if (cur.isMissingNode() || cur.isNull()) return null;
    }
    return cur;
  }

  private List<McpTool> normalizeTools(JsonNode toolsResult) {
    JsonNode toolsNode = toolsResult.path("tools");
    if (toolsNode.isMissingNode() || toolsNode.isNull()) {
      if (toolsResult.isArray()) toolsNode = toolsResult;
    }

    List<McpTool> out = new ArrayList<>();
    if (toolsNode == null || !toolsNode.isArray()) return out;

    for (JsonNode t : toolsNode) {
      String name = textOrNull(t, "name");
      String description = textOrNull(t, "description");

      JsonNode args = firstNonNull(t,
          "inputSchema",
          "arguments",
          "args",
          "parameters",
          "paramsSchema"
      );

      out.add(new McpTool(name, description, args));
    }
    return out;
  }

  private String textOrNull(JsonNode n, String field) {
    JsonNode v = n.path(field);
    if (v.isMissingNode() || v.isNull()) return null;
    return v.isTextual() ? v.asText() : v.toString();
  }

  private JsonNode firstNonNull(JsonNode n, String... fields) {
    for (String f : fields) {
      JsonNode v = n.path(f);
      if (!v.isMissingNode() && !v.isNull()) return v;
    }
    return null;
  }

  // ----------------------------
  // URL helpers (nuevo)
  // ----------------------------

  private String normalizeHttpUrl(String url) {
    String v = url.trim();

    // si alguien pega accidentalmente "https://host///" -> recortamos trailing slashes
    while (v.endsWith("/")) {
      v = v.substring(0, v.length() - 1);
    }
    return v;
  }

  private boolean isHttpScheme(String url) {
    if (url == null) return false;
    String v = url.trim().toLowerCase();
    return v.startsWith("https://") || v.startsWith("http://");
  }

  /**
   * Si URL empieza con https:// devuelve http://..., y viceversa.
   * Si no es http(s), devuelve null.
   */
  private String alternateScheme(String url) {
    if (url == null) return null;
    String v = url.trim();
    if (v.regionMatches(true, 0, "https://", 0, "https://".length())) {
      return "http://" + v.substring("https://".length());
    }
    if (v.regionMatches(true, 0, "http://", 0, "http://".length())) {
      return "https://" + v.substring("http://".length());
    }
    return null;
  }
}
