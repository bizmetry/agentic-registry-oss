package com.bizmetry.registry.dto.mcpCentral;

public class McpServerSummaryDTO {

    private String name;           // Nombre del agente
    private String version;        // Versión del agente
    private String description;    // Descripción del agente
    private String discoveryUrl;   // URL de discovery

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
}
