package com.bizmetry.registry.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mcp_servers", uniqueConstraints = {
    @UniqueConstraint(name = "uk_mcp_servers_name_version", columnNames = {"name", "version"})
})
public class McpServer {

   @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "server_id")
  private UUID serverId;

  @Column(nullable = false, length = 256)
  private String name;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 64)
  private String version;

  @Column(name = "discovery_url", nullable = false, length = 1024)
  private String discoveryUrl;

  // ✅ NEW: GitHub repository URL (optional)
  @Column(name = "repository_url", length = 512)
  private String repositoryUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "server_doc", nullable = false, columnDefinition = "jsonb")
  private JsonNode serverDoc;

  @Column(name = "created_ts", nullable = false)
  private Instant createdTs = Instant.now();

  @Column(name = "updated_ts", nullable = false)
  private Instant updatedTs = Instant.now();

  // 🚀 Nuevo: estado como enum
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private McpServerStatus status = McpServerStatus.ACTIVE;

  public McpServer() {}

  @PreUpdate
  public void preUpdate() {
    this.updatedTs = Instant.now();
  }

  // ---------------------------
  // Getters & setters
  // ---------------------------

  public UUID getServerId() {
    return serverId;
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

  public String getDiscoveryUrl() {
    return discoveryUrl;
  }
  public void setDiscoveryUrl(String discoveryUrl) {
    this.discoveryUrl = discoveryUrl;
  }

  // ✅ NEW getter/setter
  public String getRepositoryUrl() {
    return repositoryUrl;
  }
  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  public JsonNode getServerDoc() {
    return serverDoc;
  }
  public void setServerDoc(JsonNode serverDoc) {
    this.serverDoc = serverDoc;
  }

  public Instant getCreatedTs() {
    return createdTs;
  }

  public Instant getUpdatedTs() {
    return updatedTs;
  }

  public McpServerStatus getStatus() {
    return status;
  }
  public void setStatus(McpServerStatus status) {
    this.status = status;
  }
}
