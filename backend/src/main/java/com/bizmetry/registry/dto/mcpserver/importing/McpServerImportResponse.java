
package com.bizmetry.registry.dto.mcpserver.importing;
import java.util.List;

import com.bizmetry.registry.dto.mcpserver.McpServerResponse;

public class McpServerImportResponse {
  public boolean ok;
  public int created;
  public int updated;
  public List<McpServerResponse> servers; // lo importado
  public String error;
}
