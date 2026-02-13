package com.bizmetry.registry.dto.mcpCentral;

public class McpCentralServerImportResponse {

    /** Nombre del MCP Server */
    private String serverName;

    /** Versión del MCP Server */
    private String serverVersion;

    /** Descripción (si existe en MCP Central) */
    private String description;

    /** Discovery URL principal (si existe) */
    private String discoveryUrl;

    /** Indica si el server fue encontrado en MCP Central */
    private boolean found;

    /** Mensaje informativo del resultado del import */
    private String message;

    // -----------------
    // Getters / Setters
    // -----------------

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String discoveryUrl) {
        this.discoveryUrl = discoveryUrl;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
