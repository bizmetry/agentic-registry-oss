package com.bizmetry.registry.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class McpTool {

    @Column(nullable = false)
    private UUID mcpServerId; // ID del servidor MCP asociado

    @Column(nullable = false)
    private String toolName; // Nombre de la herramienta MCP asociada

    // Constructor
    public McpTool(UUID mcpServerId, String toolName) {
        this.mcpServerId = mcpServerId;
        this.toolName = toolName;
    }

    // Getters & Setters
    public UUID getMcpServerId() {
        return mcpServerId;
    }

    public void setMcpServerId(UUID mcpServerId) {
        this.mcpServerId = mcpServerId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}
