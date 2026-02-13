package com.bizmetry.registry.dto.mcpserver;

import java.util.UUID;

public class ToolInvokeResponse {

  public UUID serverId;
  public String toolName;

  public boolean ok;
  public Object result;   // puede ser Map/List/String/JsonNode
  public String error;

  public Long latencyMs;
}
