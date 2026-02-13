package com.bizmetry.registry.dto.mcpCentral;

import jakarta.validation.constraints.NotBlank;

public class McpCentralServerImportRequest {

    @NotBlank(message = "serverName es obligatorio")
    private String serverName;

    @NotBlank(message = "serverVersion es obligatorio")
    private String serverVersion;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }
}
