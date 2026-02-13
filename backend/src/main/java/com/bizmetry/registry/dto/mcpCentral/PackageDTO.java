package com.bizmetry.registry.dto.mcpCentral;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos no reconocidos

public class PackageDTO {

    private String registryType;
    private String identifier;
    private TransportDTO transport;
    private List<EnvironmentVariableDTO> environmentVariables;

    // Getters and Setters
    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public TransportDTO getTransport() {
        return transport;
    }

    public void setTransport(TransportDTO transport) {
        this.transport = transport;
    }

    public List<EnvironmentVariableDTO> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<EnvironmentVariableDTO> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
}
