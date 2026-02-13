package com.bizmetry.registry.dto.mcpserver;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpServerExportEnvelope {

  private ExportServer server;

  @JsonProperty("_meta")
  private Map<String, OfficialMeta> meta;

  public ExportServer getServer() { return server; }
  public void setServer(ExportServer server) { this.server = server; }

  public Map<String, OfficialMeta> getMeta() { return meta; }
  public void setMeta(Map<String, OfficialMeta> meta) { this.meta = meta; }

  // ------------------------

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ExportServer {

    @JsonProperty("$schema")
    private String schema;

    private String name;
    private String description;
    private Repository repository;
    private String version;
    private List<Remote> remotes;

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Repository getRepository() { return repository; }
    public void setRepository(Repository repository) { this.repository = repository; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<Remote> getRemotes() { return remotes; }
    public void setRemotes(List<Remote> remotes) { this.remotes = remotes; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Repository {
    // Dejalo vacío {} si no tenés data, o agregá los campos reales si los manejás
    // Ejemplo común: url + source (github)
    private String url;
    private String source;

    public Repository() {}
    public Repository(String url, String source) {
      this.url = url;
      this.source = source;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
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

  @JsonInclude(JsonInclude.Include.NON_NULL)
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
