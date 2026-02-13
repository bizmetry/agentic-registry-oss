package com.bizmetry.registry.jobs;

import java.util.ArrayList;
import java.util.List;

import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class McpToolNormalizer {
  private McpToolNormalizer() {}

  public static List<McpToolDefinition> normalize(Object result, ObjectMapper om) {
    List<McpToolDefinition> out = new ArrayList<>();
    if (result == null) return out;

    JsonNode node = om.valueToTree(result);

    JsonNode toolsNode = node.path("tools");
    if (toolsNode.isMissingNode() || toolsNode.isNull()) {
      if (node.isArray()) toolsNode = node;
    }

    if (!toolsNode.isArray()) return out;

    for (JsonNode t : toolsNode) {
      String name = text(t, "name");
      if (name == null) continue;

      McpToolDefinition d = new McpToolDefinition();
      d.setName(name); 
      d.setDescription( text(t, "description"));
      d.setVersion(text(t, "version"));
    
      JsonNode args = firstNonNull(t, "arguments", "inputSchema", "paramsSchema", "parameters");
      if (args != null && !args.isNull() && !args.isMissingNode()) d.setArguments(args);

      out.add(d);
    }

    return out;
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) return null;
    String s = v.asText();
    return (s == null || s.isBlank()) ? null : s.trim();
  }

  private static JsonNode firstNonNull(JsonNode n, String... fields) {
    for (String f : fields) {
      JsonNode v = n.get(f);
      if (v != null && !v.isNull() && !v.isMissingNode()) return v;
    }
    return null;
  }
}
