package com.bizmetry.registry.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.*;

@Entity
@Table(name = "agents", uniqueConstraints = {
    @UniqueConstraint(name = "uk_agents_name_version", columnNames = { "name", "version" })
})
public class Agent {

   @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "agent_id")
  private UUID agentId;

  @Column(nullable = false, length = 256)
  private String name; // Nombre del agente

  @Column(length = 4000)
  private String description; // Descripción o propósito del agente

  @Column(nullable = false, length = 64)
  private String version; // Versión del agente

  @Column(name = "github_repo_url", length = 512)
  private String githubRepoUrl; // URL del repositorio GitHub

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", nullable = true, columnDefinition = "jsonb")
  private JsonNode metadata; // metadata: LLMS, tools, Discovery URL

  @Column(name = "created_ts", nullable = false)
  private Instant createdTs = Instant.now(); // Fecha de creación

  @Column(name = "updated_ts", nullable = false)
  private Instant updatedTs = Instant.now(); // Fecha de última actualización

  // Estado del agente, puede ser activo o inactivo
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private AgentStatus status = AgentStatus.ACTIVE;

  public Agent() {
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedTs = Instant.now();
  }

  // ---------------------------
  // Getters & setters
  // ---------------------------

  public void setAgentId(UUID id) {
    this.agentId = id;
  }

  public UUID getAgentId() {
    return agentId;
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

  public void setMetadata(JsonNode metadata) {
    this.metadata = metadata;
  }

  public JsonNode getMetadata() {
    return this.metadata;
  }

  public void setCreatedTs (Instant createdTs)
  {
    this.createdTs = createdTs;
  }

  public void setUpdatedTs (Instant updatedTs )
  {
    this.updatedTs = updatedTs;
  }

  public Instant getCreatedTs() {
    return createdTs;
  }

  public Instant getUpdatedTs() {
    return updatedTs;
  }

  public AgentStatus getStatus() {
    return status;
  }

  public void setStatus(AgentStatus status) {
    this.status = status;
  }
}
