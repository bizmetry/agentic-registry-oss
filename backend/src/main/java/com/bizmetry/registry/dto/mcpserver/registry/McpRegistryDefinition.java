package com.bizmetry.registry.dto.mcpserver.registry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class McpRegistryDefinition {

  private Server server;

  @JsonProperty("_meta")
  private Map<String, OfficialMeta> meta;

  // -----------------

  public Server getServer() { return server; }
  public void setServer(Server server) { this.server = server; }

  public Map<String, OfficialMeta> getMeta() { return meta; }
  public void setMeta(Map<String, OfficialMeta> meta) { this.meta = meta; }

  // =====================================================

  public static class Server {

    @JsonProperty("$schema")
    private String schema;

    private String name;
    private String description;

    /**
     * ✅ Para evitar FAIL_ON_EMPTY_BEANS y permitir {} exactamente.
     */
    private Map<String, Object> repository;

    private String version;
    private List<Remote> remotes;

    // ✅ NUEVO: tools del MCP server
    private List<Tool> tools;

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getRepository() { return repository; }
    public void setRepository(Map<String, Object> repository) { this.repository = repository; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<Remote> getRemotes() { return remotes; }
    public void setRemotes(List<Remote> remotes) { this.remotes = remotes; }

    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }
  }

  // -----------------

  public static class Tool {

    private String name;
    private String description;
    private String version;

    // dejamos argumentos como JSON flexible
    private JsonNode arguments;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public JsonNode getArguments() { return arguments; }
    public void setArguments(JsonNode arguments) { this.arguments = arguments; }
  }

  // -----------------

  public static class Remote {
    private String type;
    private String url;

    public Remote() {}

    public Remote(String type, String url) {
      this.type = type;
      this.url = url;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
  }

  // -----------------

  public static class OfficialMeta {
    private String status;
    private Instant publishedAt;
    private Instant updatedAt;
    private Boolean isLatest;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }
  }
}
