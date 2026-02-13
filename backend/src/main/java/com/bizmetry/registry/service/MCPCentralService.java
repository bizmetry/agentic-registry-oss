package com.bizmetry.registry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.bizmetry.registry.dto.mcpCentral.McpCentralServerImportResponse;
import com.bizmetry.registry.dto.mcpCentral.McpServerResponseDTO;
import com.bizmetry.registry.dto.mcpCentral.McpServerSummaryDTO;
import com.bizmetry.registry.dto.mcpCentral.ServerWrapperDTO;
import com.bizmetry.registry.dto.mcpserver.McpServerCreateRequest;
import com.bizmetry.registry.dto.mcpserver.McpServerResponse;
import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.mcpserver.McpToolDto;
import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionResponse;

@Service
public class MCPCentralService {

  // Single source of truth
  private static final String MCP_REGISTRY_URL = "https://registry.modelcontextprotocol.io";

  private final WebClient webClient;

  @Autowired
  McpConnectionService mcpConnectionService;

  @Autowired
  McpServerService mcpServerService;

  // Constructor con WebClient inyectado
  @Autowired
  public MCPCentralService(@Qualifier("insecureWebClient") WebClient insecureWebClient,
      McpConnectionService mcpConnectionService,
      McpServerService mcpServerService) {
    this.webClient = insecureWebClient;
    this.mcpConnectionService = mcpConnectionService;
    this.mcpServerService = mcpServerService;
  }

  // -----------------------------
  // Helpers: scheme + host from MCP_REGISTRY_URL
  // -----------------------------
  private String getRegistryScheme() {
    return java.net.URI.create(MCP_REGISTRY_URL).getScheme();
  }

  private String getRegistryHost() {
    return java.net.URI.create(MCP_REGISTRY_URL).getHost();
  }

  /**
   * Obtener los servidores MCP disponibles desde el MCP Central Registry.
   *
   * Backward compatible.
   *
   * @param cursor Parámetro de paginación.
   * @param limit  El límite de servidores a devolver.
   * @return La lista de servidores MCP con la información simplificada.
   */
  public List<McpServerSummaryDTO> fetchMcpServers(String cursor, int limit) {
    return fetchMcpServers(cursor, limit, null);
  }

  /**
   * Obtener los servidores MCP disponibles desde el MCP Central Registry.
   *
   * @param cursor Parámetro de paginación.
   * @param limit  El límite de servidores a devolver.
   * @param search Texto a buscar (opcional).
   * @return La lista de servidores MCP con la información simplificada.
   */
  public List<McpServerSummaryDTO> fetchMcpServers(String cursor, int limit, String search) {
    try {
      final String normalizedCursor = (cursor == null) ? "" : cursor.trim();
      final String normalizedSearch = (search == null) ? null : search.trim();
      final String searchLower = (normalizedSearch == null || normalizedSearch.isEmpty())
          ? null
          : normalizedSearch.toLowerCase(Locale.ROOT);

      McpServerResponseDTO response = webClient.get()
          .uri(uriBuilder -> {
            uriBuilder = uriBuilder
                .scheme(getRegistryScheme())
                .host(getRegistryHost())
                .path("/v0/servers")
                .queryParam("cursor", normalizedCursor)
                .queryParam("limit", limit);

            // ✅ Pasar search al upstream (si lo soporta)
            if (searchLower != null) {
              uriBuilder = uriBuilder.queryParam("search", normalizedSearch);
            }

            return uriBuilder.build();
          })
          .retrieve()
          .bodyToMono(String.class)
          .doOnNext(responseBody -> System.out.println("Raw MCP Server response: " + responseBody))
          .map(this::mapToMcpServerResponse)
          .block();

      if (response == null || response.getServers() == null) {
        return new ArrayList<>();
      }

      List<McpServerSummaryDTO> serverSummary = new ArrayList<>();

      for (ServerWrapperDTO wrapped : response.getServers()) {
        if (wrapped == null || wrapped.getServer() == null) {
          continue;
        }

        McpServerSummaryDTO thisServer = new McpServerSummaryDTO();
        thisServer.setName(wrapped.getServer().getName());
        thisServer.setVersion(wrapped.getServer().getVersion());
        thisServer.setDescription(wrapped.getServer().getDescription());

        if (wrapped.getServer().getRemotes() != null && !wrapped.getServer().getRemotes().isEmpty()) {
          thisServer.setDiscoveryUrl(wrapped.getServer().getRemotes().getFirst().getUrl());
        }

        // ✅ Filtro local defensivo (por si el upstream ignora "search")
        if (searchLower != null) {
          String name = safeLower(thisServer.getName());
          String desc = safeLower(thisServer.getDescription());
          String disc = safeLower(thisServer.getDiscoveryUrl());

          boolean matches = (name.contains(searchLower) || desc.contains(searchLower) || disc.contains(searchLower));
          if (!matches) {
            continue;
          }
        }

        serverSummary.add(thisServer);
      }

      return serverSummary;
    } catch (Exception e) {
      System.err.println("Failed to pull MCP Server from central: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to pull MCP Server from central: " + e.getMessage(), e);
    }
  }

  private String safeLower(String v) {
    return (v == null) ? "" : v.toLowerCase(Locale.ROOT);
  }

  private McpServerResponseDTO mapToMcpServerResponse(String responseBody) {
    try {
      com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);

      // Caso A) respuesta "list": { "servers": [ { "server": {...} }, ... ] }
      if (root.has("servers") && root.get("servers").isArray()) {
        McpServerResponseDTO response = objectMapper.treeToValue(root, McpServerResponseDTO.class);

        if (response != null && response.getServers() != null) {
          response.getServers().forEach(serverWrapper -> {
            if (serverWrapper != null && serverWrapper.getServer() != null) {
              System.out.println("Server data: " + serverWrapper.getServer());
            }
          });
        }

        return response;
      }

      // Caso B) respuesta "single": { "server": {...}, "_meta": {...} }
      if (root.has("server") && root.get("server").isObject()) {
        // Parseo el server directamente a ServerDTO
        com.bizmetry.registry.dto.mcpCentral.ServerDTO server = objectMapper.treeToValue(root.get("server"),
            com.bizmetry.registry.dto.mcpCentral.ServerDTO.class);

        // Lo envuelvo en un ServerWrapperDTO para mantener compatibilidad con
        // McpServerResponseDTO
        ServerWrapperDTO wrapper = new ServerWrapperDTO();
        wrapper.setServer(server);

        List<ServerWrapperDTO> servers = new ArrayList<>();
        servers.add(wrapper);

        McpServerResponseDTO response = new McpServerResponseDTO();
        response.setServers(servers);

        // (Opcional) si McpServerResponseDTO soporta meta y querés guardarlo, tipalo y
        // setealo acá.
        System.out.println("Server data: " + server);

        return response;
      }

      // Si no matchea ninguna forma conocida:
      throw new RuntimeException("Unrecognized MCP Central response shape");

    } catch (Exception e) {
      System.err.println("Error al mapear la respuesta cruda al DTO: " + e.getMessage());
      throw new RuntimeException("Error parsing MCP Central response", e);
    }
  }

  /**
   * Importar agentes de manera ficticia (dummy import).
   * Este método simula el proceso de importación de agentes desde un servidor
   * MCP.
   *
   * @param mcpServerId El ID del servidor MCP desde el cual se realizará la
   *                    importación.
   * @return True si la importación se simula correctamente.
   */
  public boolean importAgentsFromMcp(String mcpServerId) {
    try {
      System.out.println("Simulando la importación de agentes desde el servidor MCP: " + mcpServerId);
      return true;
    } catch (Exception e) {
      System.err.println("Error durante la importación de agentes desde MCP: " + e.getMessage());
      return false;
    }
  }

  /**
   * Fetch de un MCP Server específico desde MCP Central dado name + version.
   *
   * Upstream:
   * GET /v0.1/servers/{serverName}/versions/{version}
   *
   * @param serverName Nombre del server (path param)
   * @param version    Versión del server (path param)
   * @return McpServerResponseDTO (respuesta del central)
   */
  public McpServerResponseDTO fetchMcpServerByNameAndVersion(String serverName, String version) {
    try {
      if (serverName == null || serverName.trim().isEmpty()) {
        throw new IllegalArgumentException("serverName no puede estar vacío");
      }
      if (version == null || version.trim().isEmpty()) {
        throw new IllegalArgumentException("version no puede estar vacío");
      }

      final String normalizedName = serverName.trim();
      final String normalizedVersion = version.trim();

      McpServerResponseDTO response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .scheme(getRegistryScheme())
              .host(getRegistryHost())
              .path("/v0.1/servers/{serverName}/versions/{version}")
              .build(normalizedName, normalizedVersion))
          .retrieve()
          .bodyToMono(String.class)
          .doOnNext(raw -> System.out.println("Raw MCP Server (by name/version) response: " + raw))
          .map(this::mapToMcpServerResponse)
          .block();

      return response;
    } catch (Exception e) {
      System.err.println("Failed to fetch MCP Server from central (name=" + serverName + ", version=" + version + "): "
          + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(
          "Failed to fetch MCP Server from central (name=" + serverName + ", version=" + version + "): "
              + e.getMessage(),
          e);
    }
  }

  public McpServerResponse importMcpServer(String serverName, String serverVersion) {

    // 1) Fetch desde MCP Central (name + version)
    McpServerResponseDTO serverDefinition = fetchMcpServerByNameAndVersion(serverName, serverVersion);

    if (serverDefinition == null || serverDefinition.getServers() == null || serverDefinition.getServers().isEmpty()) {
      throw new IllegalArgumentException(
          "MCP Server not found in central registry (name=" + serverName + ", version=" + serverVersion + ")");
    }

    ServerWrapperDTO wrapper = serverDefinition.getServers().get(0);

    if (wrapper == null || wrapper.getServer() == null) {
      throw new IllegalStateException(
          "Invalid MCP Central response for server " + serverName + ":" + serverVersion);
    }

    if (wrapper.getServer().getRemotes() == null || wrapper.getServer().getRemotes().isEmpty()
        || wrapper.getServer().getRemotes().getFirst() == null
        || wrapper.getServer().getRemotes().getFirst().getUrl() == null
        || wrapper.getServer().getRemotes().getFirst().getUrl().isBlank()) {
      throw new IllegalStateException(
          "MCP Central response has no discovery url for " + serverName + ":" + serverVersion);
    }

    String discoveryURL = wrapper.getServer().getRemotes().getFirst().getUrl().trim();

    // 2) Conectar al MCP Server para obtener tools/detalles
    McpServerTestConnectionResponse connectionResponse = mcpConnectionService.testConnection(discoveryURL);

    if (connectionResponse == null || !connectionResponse.isOk()) {
      throw new IllegalArgumentException("La URL de discovery no es válida o no responde: " + discoveryURL);
    }

    // 3) Mapear tools desde la respuesta del test connection
    List<McpToolDefinition> tools = new ArrayList<>();
    if (connectionResponse.getTools() != null) {
      for (McpToolDto tool : connectionResponse.getTools()) {
        McpToolDefinition tooldef = new McpToolDefinition().builder()
            .arguments(tool.getArguments())
            .description(tool.getDescription())
            .name(tool.getName())
            .build();

        tools.add(tooldef);
      }
    }

    // 4) Construir request para persistir (repository puede venir vacío =>
    // null-safe)
    String repositoryUrl = null;
    if (wrapper.getServer().getRepository() != null) {
      repositoryUrl = wrapper.getServer().getRepository().getUrl();
    }

    McpServerCreateRequest thisRequest = new McpServerCreateRequest().builder()
        .description(wrapper.getServer().getDescription())
        .discoveryUrl(discoveryURL)
        .name(wrapper.getServer().getName())
        .repositoryUrl(repositoryUrl)
        .version(wrapper.getServer().getVersion())
        .tools(tools)
        .build();

    // 5) Persistir el MCP server en la base de datos
    McpServerResponse saved = mcpServerService.register(thisRequest);
 return saved;
  }
}
