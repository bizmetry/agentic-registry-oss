package com.bizmetry.registry.dto.mcpserver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public class ToolInvokeRequest {
  public Object args;
  public Integer timeoutMs;
  public Boolean dryRun;

  @Valid
  public Auth auth;

  public static class Auth {
    /**
     * Valor raw del bearer token SIN el prefijo "Bearer ".
     * (en UI pedimos solo el token; si el user pega "Bearer xxx", lo normalizamos)
     */
    @Size(max = 8192)
    public String bearerToken;
  }
}
