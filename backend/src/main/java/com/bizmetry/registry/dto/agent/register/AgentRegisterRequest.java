package com.bizmetry.registry.dto.agent.register;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public class AgentRegisterRequest {

    @NotBlank(message = "El nombre del agente no puede estar vacío")
    private String agentName;

    private String description;

    @NotBlank(message = "La versión del agente no puede estar vacía")
    private String version;

    private String githubRepoUrl;

    private Discovery discovery;

    private List<String> llms;

    private List<Tool> tools;

    // Getters y Setters

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
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

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public List<String> getLlms() {
        return llms;
    }

    public void setLlms(List<String> llms) {
        this.llms = llms;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    // Clases internas

    public static class Discovery {
        private String url;
        private String method;
        private String queryParam;

        // Getters y Setters

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }
    }

    public static class Tool {
        private String serverName;
        private String toolName;
        private String serverVersion;

        // Getters y Setters

        public String getServerName() {
            return serverName;
        }

        public String getServerVersion () {
            return serverVersion;
        }

        public void setServerVersion (String version)
        {
            this.serverVersion = version;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
    }
}
