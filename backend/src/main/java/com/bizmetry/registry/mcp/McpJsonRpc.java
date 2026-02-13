package com.bizmetry.registry.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class McpJsonRpc {
  private McpJsonRpc() {}

  // ✅ default recomendado (coincide con lo que ya viste en metadata)
  public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

  public static ObjectNode initialize(ObjectMapper mapper) {
    return initialize(mapper, DEFAULT_PROTOCOL_VERSION);
  }

  public static ObjectNode initialize(ObjectMapper mapper, String protocolVersion) {
    if (protocolVersion == null || protocolVersion.isBlank()) {
      protocolVersion = DEFAULT_PROTOCOL_VERSION;
    }

    ObjectNode root = mapper.createObjectNode();
    root.put("jsonrpc", "2.0");
    root.put("id", 1);
    root.put("method", "initialize");

    ObjectNode params = mapper.createObjectNode();

    // ✅ REQUIRED por algunos MCP servers (ej: exa)
    params.put("protocolVersion", protocolVersion);

    // clientInfo
    ObjectNode clientInfo = mapper.createObjectNode();
    clientInfo.put("name", "bizmetry-agent-registry");
    clientInfo.put("version", "1.0.0");
    params.set("clientInfo", clientInfo);

    // ✅ Capabilities mínimas (estructura esperada)
    ObjectNode caps = mapper.createObjectNode();

    // Algunos servers validan que existan estos nodos, aunque estén vacíos.
    caps.set("tools", mapper.createObjectNode());
    caps.set("prompts", mapper.createObjectNode());

    ObjectNode resources = mapper.createObjectNode();
    // si querés, podés declarar intención de subscribe:
    resources.put("subscribe", true);
    caps.set("resources", resources);

    // opcionales pero vistos en tu metadata anterior:
    ObjectNode resourcesTemplates = mapper.createObjectNode();
    resourcesTemplates.put("subscribe", true);
    caps.set("resourcesTemplates", resourcesTemplates);

    caps.set("logging", mapper.createObjectNode());

    params.set("capabilities", caps);

    root.set("params", params);
    return root;
  }

  public static ObjectNode toolsList(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("jsonrpc", "2.0");
    root.put("id", 2);
    root.put("method", "tools/list");
    root.set("params", mapper.createObjectNode());
    return root;
  }
}
