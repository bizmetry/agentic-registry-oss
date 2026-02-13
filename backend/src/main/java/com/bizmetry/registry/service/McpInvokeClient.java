package com.bizmetry.registry.service;

import java.util.Map;

import com.bizmetry.registry.model.McpServer;

public interface McpInvokeClient {

  // ============================================================
  // Tool invocation
  // ============================================================

  // ✅ existente (compatibilidad)
  Object invokeTool(McpServer server, String toolName, Map<String, Object> args, Long timeoutMs);

  // ✅ NUEVO: soporta OAuth bearer token opcional
  Object invokeTool(
      McpServer server,
      String toolName,
      Map<String, Object> args,
      Long timeoutMs,
      String bearerToken
  );

  // ============================================================
  // Generic JSON-RPC call
  // ============================================================

  // ✅ existente (compatibilidad)
  Object call(McpServer server, String method, Map<String, Object> params, Long timeoutMs);

  // ✅ NUEVO: soporta OAuth bearer token opcional
  Object call(
      McpServer server,
      String method,
      Map<String, Object> params,
      Long timeoutMs,
      String bearerToken
  );
}
