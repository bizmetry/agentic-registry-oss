package com.bizmetry.registry.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.bizmetry.registry.model.McpServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class HttpMcpInvokeClient implements McpInvokeClient {

  private final WebClient webClient;
  private final ObjectMapper om;

  public HttpMcpInvokeClient(WebClient.Builder builder, ObjectMapper om) {
    this.webClient = builder.build();
    this.om = om;
  }

  // ============================================================
  // invokeTool
  // ============================================================

  @Override
  public Object invokeTool(McpServer server, String toolName, Map<String, Object> args, Long timeoutMs) {
    return invokeTool(server, toolName, args, timeoutMs, null);
  }

  @Override
  public Object invokeTool(McpServer server, String toolName, Map<String, Object> args, Long timeoutMs, String bearerToken) {
    Map<String, Object> params = Map.of(
        "name", toolName,
        "arguments", (args != null ? args : Map.of())
    );
    return call(server, "tools/call", params, timeoutMs, bearerToken);
  }

  // ============================================================
  // call
  // ============================================================

  @Override
  public Object call(McpServer server, String method, Map<String, Object> params, Long timeoutMs) {
    return call(server, method, params, timeoutMs, null);
  }

  @Override
  public Object call(McpServer server, String method, Map<String, Object> params, Long timeoutMs, String bearerToken) {

    String endpoint = server.getDiscoveryUrl();
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalArgumentException("MCP Server discoveryUrl is empty");
    }

    if (method == null || method.isBlank()) {
      throw new IllegalArgumentException("MCP method is required");
    }

    long id = System.nanoTime();

    Map<String, Object> body = Map.of(
        "jsonrpc", "2.0",
        "id", id,
        "method", method,
        "params", (params != null ? params : Map.of())
    );

    Duration timeout = Duration.ofMillis(timeoutMs != null ? timeoutMs : 30000);

    String token = normalizeBearer(bearerToken);

    WebClient.RequestBodySpec req = webClient.post()
        .uri(endpoint.trim())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM) // ✅ evita 406 en servers SSE
        .header(HttpHeaders.CACHE_CONTROL, "no-cache");

    // ✅ Solo si viene token: Authorization: Bearer <token>
    if (token != null) {
      req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    return req
        .bodyValue(body)
        .exchangeToMono(resp -> handleResponse(resp, endpoint))
        .timeout(timeout)
        .block();
  }

  // ============================================================
  // helpers
  // ============================================================

  private String normalizeBearer(String bearerToken) {
    if (bearerToken == null) return null;
    String t = bearerToken.trim();
    if (t.isEmpty()) return null;

    // Si pegaron "Bearer xxx", lo normalizamos a "xxx"
    if (t.regionMatches(true, 0, "Bearer ", 0, 7)) {
      t = t.substring(7).trim();
    }

    return t.isEmpty() ? null : t;
  }

  private Mono<Object> handleResponse(ClientResponse resp, String endpoint) {

    int status = resp.statusCode().value();
    MediaType ct = resp.headers().contentType().orElse(null);

    return resp.bodyToMono(String.class).defaultIfEmpty("")
        .flatMap(raw -> {
          if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP " + status + " from POST " + endpoint + " -> " + truncate(raw));
          }

          try {
            String payloadText = raw;

            if (isEventStream(ct, raw)) {
              String jsonFromSse = extractJsonFromSse(raw);
              if (jsonFromSse == null || jsonFromSse.isBlank()) {
                throw new RuntimeException("SSE response received but no JSON payload found from " + endpoint
                    + " -> " + truncate(raw));
              }
              payloadText = jsonFromSse;
            }

            Map<String, Object> map = om.readValue(payloadText, new TypeReference<Map<String, Object>>() {});
            if (map == null) return Mono.justOrEmpty(null);

            if (map.containsKey("result")) return Mono.just(map.get("result"));

            if (map.containsKey("error")) {
              throw new RuntimeException(String.valueOf(map.get("error")));
            }

            return Mono.just(map);

          } catch (Exception e) {
            throw new RuntimeException("Unable to parse MCP response from " + endpoint + ": " + e.getMessage()
                + " | body=" + truncate(raw), e);
          }
        });
  }

  private boolean isEventStream(MediaType ct, String body) {
    if (ct != null && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(ct)) return true;
    if (body == null) return false;
    String t = body.trim();
    return t.startsWith("event:") || t.startsWith("data:") || t.startsWith("id:");
  }

  private String extractJsonFromSse(String sseBody) {
    if (sseBody == null || sseBody.isBlank()) return null;

    String[] lines = sseBody.split("\\r?\\n");
    StringBuilder dataBuf = new StringBuilder();
    String lastGoodJson = null;

    for (String line : lines) {
      if (line.startsWith("data:")) {
        String chunk = line.substring("data:".length()).trim();
        if (!chunk.isEmpty()) dataBuf.append(chunk).append("\n");
        continue;
      }

      if (line.trim().isEmpty() && dataBuf.length() > 0) {
        String candidate = dataBuf.toString().trim();
        dataBuf.setLength(0);

        if (candidate.equals("[DONE]")) continue;

        try {
          om.readTree(candidate);
          lastGoodJson = candidate;
        } catch (Exception ignore) {}
      }
    }

    if (lastGoodJson == null && dataBuf.length() > 0) {
      String candidate = dataBuf.toString().trim();
      try {
        om.readTree(candidate);
        lastGoodJson = candidate;
      } catch (Exception ignore) {}
    }

    return lastGoodJson;
  }

  private String truncate(String s) {
    if (s == null) return "";
    return s.length() > 800 ? s.substring(0, 800) + "..." : s;
  }
}
