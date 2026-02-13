package com.bizmetry.registry.dto.agent;

import java.util.List;
import java.util.UUID;

import com.bizmetry.registry.dto.agent.MetadataDTO.Discovery;
import com.bizmetry.registry.dto.agent.MetadataDTO.LLMInfo;
import com.fasterxml.jackson.databind.JsonNode;

public class AgentSnapshotDTO {

    private UUID agentId;
    private String name;
    private String description;
    private String version;
    private String githubRepoUrl;
    private String createdTs;
    private String updatedTs;
    private ConfigDTO config;  // Referencia a la clase config

    // Getters and setters

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGithubRepoUrl() {
        return githubRepoUrl;
    }

    public void setGithubRepoUrl(String githubRepoUrl) {
        this.githubRepoUrl = githubRepoUrl;
    }

    public String getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(String createdTs) {
        this.createdTs = createdTs;
    }

    public String getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(String updatedTs) {
        this.updatedTs = updatedTs;
    }

    public ConfigDTO getConfig() {
        return config;
    }

    public void setConfig(ConfigDTO config) {
        this.config = config;
    }

    // Inner DTO for Config
    public static class ConfigDTO {

        private List<McpServerDTO> mcpServers;
        private List<LLMInfo> llmInfo; // Referencia directamente a LLMInfo desde MetadataDTO
        private Discovery discovery; // Referencia directamente a Discovery desde MetadataDTO

        // Getters and setters

        public List<McpServerDTO> getMcpServers() {
            return mcpServers;
        }

        public void setMcpServers(List<McpServerDTO> mcpServers) {
            this.mcpServers = mcpServers;
        }

        public List<LLMInfo> getLlmInfo() {
            return llmInfo;
        }

        public void setLlmInfo(List<LLMInfo> llmInfo) {
            this.llmInfo = llmInfo;
        }

        public Discovery getDiscovery() {
            return discovery;
        }

        public void setDiscovery(Discovery discovery) {
            this.discovery = discovery;
        }

        // Inner DTO for MCP Server Details
        public static class McpServerDTO {

            private UUID mcpServerId;
            private String mcpServerName;
            private String mcpServerVersion;
            private String description;
            private String version;
            private String discoveryUrl;
            private String repositoryUrl;
            private List<ToolDTO> tools;

            // Getters and setters

            public UUID getMcpServerId() {
                return mcpServerId;
            }

            public void setMcpServerId(UUID mcpServerId) {
                this.mcpServerId = mcpServerId;
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

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            public String getDiscoveryUrl() {
                return discoveryUrl;
            }

            public void setDiscoveryUrl(String discoveryUrl) {
                this.discoveryUrl = discoveryUrl;
            }

            public String getRepositoryUrl() {
                return repositoryUrl;
            }

            public void setRepositoryUrl(String repositoryUrl) {
                this.repositoryUrl = repositoryUrl;
            }

            public List<ToolDTO> getTools() {
                return tools;
            }

            public void setTools(List<ToolDTO> tools) {
                this.tools = tools;
            }

            // Inner DTO for Tool Details
            public static class ToolDTO {

                private String name;
                private String description;
                private String version;
                private JsonNode arguments; // Usando JsonNode para los argumentos

                // Getters and setters

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getDescription() {
                    return description;
                }

                public void setDescription(String description) {
                    this.description = description;
                }

                public String getVersion() {
                    return version;
                }

                public void setVersion(String version) {
                    this.version = version;
                }

                public JsonNode getArguments() {
                    return arguments;
                }

                public void setArguments(JsonNode arguments) {
                    this.arguments = arguments;
                }
            }
        }

        // No es necesario redefinir LLMInfo y Discovery, ya que ya están definidos en MetadataDTO
    }
}
