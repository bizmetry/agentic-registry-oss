package com.bizmetry.registry.dto.agent;

import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public class AgentDTO {

    private UUID agentId;
    private String name;
    private String description;
    private String version;
    private String githubRepoUrl;
    private MetadataDTO metadata; // Metadata en formato JSON
    private Instant createdTs;
    private Instant updatedTs;
    private AgentStatus status; // Estado del agente

    // Constructor vacío
    public AgentDTO() {
    }

    // Constructor con parámetros
    public AgentDTO(UUID agentId, String name, String description, String version, String githubRepoUrl,
            MetadataDTO metadata, Instant createdTs, Instant updatedTs, AgentStatus status) {
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.version = version;
        this.githubRepoUrl = githubRepoUrl;
        this.metadata = metadata;
        this.createdTs = createdTs;
        this.updatedTs = updatedTs;
        this.status = status;
    }

    // Getters and Setters
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

    public MetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataDTO metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(Instant createdTs) {
        this.createdTs = createdTs;
    }

    public Instant getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(Instant updatedTs) {
        this.updatedTs = updatedTs;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    // Método para convertir un AgentDTO a una entidad Agent
    public Agent toEntity() {
        // Crear la entidad Agent a partir del DTO
        Agent agent = new Agent();
        agent.setAgentId(this.agentId);
        agent.setName(this.name);
        agent.setDescription(this.description);
        agent.setVersion(this.version);
        agent.setGithubRepoUrl(this.githubRepoUrl);
        agent.setMetadata(this.metadata.convertToJsonNode());
        agent.setCreatedTs(this.createdTs);
        agent.setUpdatedTs(this.updatedTs);
        agent.setStatus((this.status));

        return agent;
    }

    // Método para convertir un Agent a un AgentDTO
    public static AgentDTO fromEntity(Agent agent) {
        return new AgentDTO(
                agent.getAgentId(),
                agent.getName(),
                agent.getDescription(),
                agent.getVersion(),
                agent.getGithubRepoUrl(),
                MetadataDTO.fromJsonNode(agent.getMetadata()),

                agent.getCreatedTs(),
                agent.getUpdatedTs(),
                agent.getStatus());
    }
}
