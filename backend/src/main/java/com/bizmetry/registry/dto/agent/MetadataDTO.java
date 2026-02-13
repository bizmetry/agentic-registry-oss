package com.bizmetry.registry.dto.agent;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataDTO {

    private List<LLMInfo> llms; // Lista de LLMs asociadas
    private List<McpTool> tools; // Lista de herramientas MCP asociadas
    private Discovery discovery; // Información sobre el discovery

    // Getters and Setters
    public List<LLMInfo> getLlms() {
        return llms;
    }

    public void setLlms(List<LLMInfo> llms) {
        this.llms = llms;
    }

    public List<McpTool> getTools() {
        return tools;
    }

    public void setTools(List<McpTool> tools) {
        this.tools = tools;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    // Convertir MetadataDTO a JsonNode
    public JsonNode convertToJsonNode() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.valueToTree(this);
    }

    // Convertir un JsonNode a MetadataDTO
    public static MetadataDTO fromJsonNode(JsonNode node) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(node, MetadataDTO.class);
    }

    // Clases internas para cada sección de la metadata

    // LLMInfo
    public static class LLMInfo {
        private UUID id; // ID del modelo LLM
        private String modelFamily; // Familia del modelo (por ejemplo, GPT)
        private String modelName; // Nombre del modelo (por ejemplo, "GPT-4")

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getModelFamily() {
            return modelFamily;
        }

        public void setModelFamily(String modelFamily) {
            this.modelFamily = modelFamily;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }

    // McpTool
    public static class McpTool {
        private UUID mcpServerId; // ID del servidor MCP
        private String toolName; // Nombre de la herramienta MCP
        private String mcpServerName; // Nombre del servidor MCP
        private String mcpServerVersion; // Versión del servidor MCP

        // Getters and Setters
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

        public String getMcpServerName() {
            return mcpServerName;
        }

        public void setMcpServerName(String mcpServerName) {
            this.mcpServerName = mcpServerName;
        }

        public String getMcpServerVersion() {
            return mcpServerVersion;
        }

        public void setMcpServerVersion(String mcpServerVersion) {
            this.mcpServerVersion = mcpServerVersion;
        }
    }

    // Discovery
    public static class Discovery {
        private String method; // Método HTTP (POST, GET, etc.)
        private String protocol; // Protocolo (HTTP o HTTPS)
        private String endpoint; // Endpoint donde el agente está expuesto
        private String queryParam; // El query parameter para enviar al agente

        // Getters and Setters
        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }
    }
}
