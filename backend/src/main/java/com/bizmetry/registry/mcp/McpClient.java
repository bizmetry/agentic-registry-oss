package com.bizmetry.registry.mcp;

import java.util.List;

public interface McpClient {
  McpSession initialize(String discoveryUrl);
  List<McpTool> listTools(McpSession session);
}
