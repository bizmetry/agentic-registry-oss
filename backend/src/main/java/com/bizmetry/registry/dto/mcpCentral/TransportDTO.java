package com.bizmetry.registry.dto.mcpCentral;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos no reconocidos

public class TransportDTO {

    private String type;

    // Getter and Setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

