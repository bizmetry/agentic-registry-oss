package com.bizmetry.registry.dto.mcpCentral;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos no reconocidos
public class McpServerResponseDTO {

    private List<ServerWrapperDTO> servers; // Cambiamos la lista de ServerDTO a ServerWrapperDTO

    public List<ServerWrapperDTO> getServers() {
        return servers;
    }

    public void setServers(List<ServerWrapperDTO> servers) {
        this.servers = servers;
    }
}
