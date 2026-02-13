package com.bizmetry.registry.dto.mcpCentral;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos no reconocidos

public class ServerWrapperDTO {

    private ServerDTO server; // El campo 'server' en la respuesta cruda

    public ServerDTO getServer() {
        return server;
    }

    public void setServer(ServerDTO server) {
        this.server = server;
    }
}
