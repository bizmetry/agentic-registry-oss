package com.bizmetry.registry.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ToolRef {

  @NotNull
  public UUID serverId;

  @NotBlank @Size(max = 256)
  public String toolName;

  @NotBlank @Size(max = 64)
  public String toolVersion;
}
