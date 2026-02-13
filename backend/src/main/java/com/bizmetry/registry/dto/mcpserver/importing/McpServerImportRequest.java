package com.bizmetry.registry.dto.mcpserver.importing;

import jakarta.validation.constraints.NotNull;

public class McpServerImportRequest {
  @NotNull
  public Object payload; // puede ser Map/JsonNode; lo convertimos con ObjectMapper
  public Boolean dryRun; // opcional: true = solo valida/preview
  public Boolean upsert; // opcional: default true
}
