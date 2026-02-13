package com.bizmetry.registry.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.bizmetry.registry.dto.agent.AgentDTO;
import com.bizmetry.registry.dto.agent.AgentEndpointTestRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestResponse;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO.ConfigDTO;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO.ConfigDTO.McpServerDTO;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO.ConfigDTO.McpServerDTO.ToolDTO;
import com.bizmetry.registry.dto.agent.MetadataDTO;
import com.bizmetry.registry.dto.agent.MetadataDTO.McpTool;
import com.bizmetry.registry.dto.mcpserver.McpToolDefinition;
import com.bizmetry.registry.dto.mcpserver.connection.McpServerTestConnectionResponse;
import com.bizmetry.registry.jobs.McpToolNormalizer;
import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.AgentStatus;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.repo.AIModelRepository;
import com.bizmetry.registry.repo.AgentRepository;
import com.bizmetry.registry.repo.McpServerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class AgentService {

  private final AgentRepository agentRepository;
  private final McpServerRepository mcpServerRepository;
  private final WebClient webClient;
  private final WebClient insecureWebClient;
  private final ObjectMapper om;
  private final McpConnectionService mcpConnectionService;

  public AgentService(
      AgentRepository agentRepository,
      McpServerRepository mcpServerRepository,
      WebClient webClient,
      McpServerService mcpServerService,
      ObjectMapper om,
      McpConnectionService mcpConnectionService,
      AIModelRepository aiModelRepository,
      @Qualifier("insecureWebClient") WebClient insecureWebClient) {
    this.agentRepository = agentRepository;
    this.mcpServerRepository = mcpServerRepository;
    this.webClient = webClient;
    this.insecureWebClient = insecureWebClient;
    this.mcpConnectionService = mcpConnectionService;

    this.om = om;
  }

  /**
   * Crea un nuevo agente, validando antes que no exista un agente con el mismo
   * nombre y versión, y que las herramientas y servidores MCP sean válidos.
   *
   * @param agentDTO El DTO con la información del agente a crear.
   * @return El DTO del agente creado.
   */
  public AgentDTO createAgent(AgentDTO agentDTO) {
    // Validar que no exista un agente con el mismo nombre y versión
    Optional<Agent> existingAgent = agentRepository.findByNameAndVersion(agentDTO.getName(), agentDTO.getVersion());
    if (existingAgent.isPresent()) {
      throw new IllegalArgumentException("An agent with the same name and version already exists.");
    }

    // Deserializar la metadata JSON a MetadataDTO
    MetadataDTO metadata = agentDTO.getMetadata();

    // Validar que las tools declaradas existan en la registry
    for (MetadataDTO.McpTool tool : metadata.getTools()) {

      // Buscar el MCP Server correspondiente por ID
      Optional<McpServer> mcpServer = mcpServerRepository.findById(tool.getMcpServerId());

      if (!mcpServer.isPresent()) {
        throw new IllegalArgumentException("MCP Server with ID " + tool.getMcpServerId() + " does not exist.");
      }

      tool.setMcpServerName(mcpServer.get().getName());
      tool.setMcpServerVersion(mcpServer.get().getVersion());

      // Obtener el serverDoc que contiene las herramientas del MCP Server
      JsonNode serverDoc = mcpServer.get().getServerDoc();

      // Verificar que la herramienta esté presente en el serverDoc
      boolean toolFound = false;
      if (serverDoc != null && serverDoc.has("tools")) {
        JsonNode toolsNode = serverDoc.get("tools");

        // Iterar sobre las herramientas y verificar si la herramienta requerida está
        // presente
        for (JsonNode toolNode : toolsNode) {
          String toolName = toolNode.get("name").asText();
          if (toolName.equals(tool.getToolName())) {
            toolFound = true;
            break;
          }
        }
      }

      if (!toolFound) {
        throw new IllegalArgumentException(
            "Tool with name " + tool.getToolName() + " does not exist in MCP Server with ID " + tool.getMcpServerId());
      }
    }

    // Convertir el DTO a entidad Agent
    Agent agent = agentDTO.toEntity();
    agent.setCreatedTs(Instant.now());
    agent.setStatus(AgentStatus.ACTIVE);
    agent.setUpdatedTs(Instant.now());

    // Guardar el agente en la base de datos
    Agent savedAgent = agentRepository.save(agent);

    // Convertir la entidad guardada de vuelta a DTO
    return AgentDTO.fromEntity(savedAgent);
  }

  public AgentEndpointTestResponse testEndpoint(AgentEndpointTestRequest req) {
    AgentEndpointTestResponse out = new AgentEndpointTestResponse();

    String method = safeUpper(req.getMethod());
    String protocol = safeUpper(req.getProtocol());
    String endpoint = safeTrim(req.getEndpoint());
    String queryParam = safeTrim(req.getQueryParam());
    String token = safeTrim(req.getBearerToken()); // opcional
    String query = safeTrim(req.getQuery());
    if (query.isEmpty())
      query = "test";

    // server-side validation
    HttpMethod httpMethod;
    try {
      httpMethod = HttpMethod.valueOf(method);
    } catch (Exception e) {
      out.setOk(false);
      out.setStatus(400);
      out.setMessage("Invalid method. Allowed: GET, POST, PUT.");
      return out;
    }

    if (!protocol.equals("HTTP") && !protocol.equals("HTTPS")) {
      out.setOk(false);
      out.setStatus(400);
      out.setMessage("Invalid protocol. Allowed: HTTP, HTTPS.");
      return out;
    }

    if (endpoint.isEmpty() || queryParam.isEmpty()) {
      out.setOk(false);
      out.setStatus(400);
      out.setMessage("endpoint and queryParam are required.");
      return out;
    }

    if (!(httpMethod == HttpMethod.GET || httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT)) {
      out.setOk(false);
      out.setStatus(400);
      out.setMessage("Invalid method. Allowed: GET, POST, PUT.");
      return out;
    }

    String qp = URLEncoder.encode(queryParam, StandardCharsets.UTF_8);
    String qv = URLEncoder.encode(query, StandardCharsets.UTF_8);

    // ✅ URL: siempre agregamos queryParam en la URL (GET/POST/PUT)
    String base = protocol.toLowerCase() + "://" + endpoint;
    String join;
    if (base.contains("?")) {
      if (base.endsWith("?") || base.endsWith("&"))
        join = "";
      else
        join = "&";
    } else {
      join = "?";
    }
    String url = base + join + qp + "=" + qv;

    // ✅ authHeader solo si hay token
    String authHeader = null;
    if (!token.isEmpty()) {
      authHeader = token.toLowerCase().startsWith("bearer ") ? token : "Bearer " + token;
    }

    // ✅ POST/PUT: body JSON { "<queryParam>": "<query>" }
    Map<String, Object> body = Map.of(queryParam, query);

    long start = System.nanoTime();

    try {
      WebClient client = "HTTPS".equals(protocol) ? insecureWebClient : webClient;

      WebClient.RequestBodySpec spec = client
          .method(httpMethod)
          .uri(URI.create(url))
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON);

      if (authHeader != null) {
        spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
      }

      Mono<ClientResponse> call;
      if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
        // ✅ además de queryParam en URL, lo mandamos en el JSON body
        call = spec.bodyValue(body).exchangeToMono(Mono::just);
      } else {
        // GET: sin body
        call = spec.exchangeToMono(Mono::just);
      }

      ClientResponse resp = call
          .timeout(Duration.ofSeconds(6))
          .block();

      if (resp == null) {
        out.setOk(false);
        out.setStatus(0);
        out.setMessage("No response (timeout or connection issue).");
        return out;
      }

      int status = resp.statusCode().value();
      String text = resp.bodyToMono(String.class).defaultIfEmpty("")
          .block(Duration.ofSeconds(3));

      long latencyMs = (System.nanoTime() - start) / 1_000_000;

      out.setStatus(status);
      out.setLatencyMs(latencyMs);
      out.setResponseSnippet(snippet(text, 1200));

      boolean ok = status >= 200 && status < 300;
      out.setOk(ok);
      out.setMessage(ok ? "Endpoint reachable. Test invocation succeeded."
          : "Endpoint responded with non-2xx status.");
      return out;

    } catch (WebClientResponseException e) {
      long latencyMs = (System.nanoTime() - start) / 1_000_000;
      out.setOk(false);
      out.setStatus(e.getRawStatusCode());
      out.setLatencyMs(latencyMs);
      out.setMessage("Endpoint responded with error status.");
      out.setResponseSnippet(snippet(e.getResponseBodyAsString(), 1200));
      return out;

    } catch (Exception e) {
      out.setOk(false);
      out.setStatus(0);
      out.setLatencyMs((System.nanoTime() - start) / 1_000_000);
      out.setMessage("Endpoint test failed: " + e.getMessage());
      return out;
    }
  }

  private static String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String safeUpper(String s) {
    return safeTrim(s).toUpperCase();
  }

  private static String snippet(String s, int max) {
    if (s == null)
      return "";
    String t = s.trim();
    if (t.length() <= max)
      return t;
    return t.substring(0, max) + " ...";
  }

  public AgentSnapshotDTO getAgentSnapshot(UUID id) {
    // 1. Obtener el agente por su ID
    Agent agent = agentRepository.findById(id).orElseThrow(() -> new RuntimeException("Agent not found"));

    // 2. Verificar si el agente está activo
    if (!agent.getStatus().equals(AgentStatus.ACTIVE)) {
      throw new RuntimeException("Agent is not active");
    }

    // Convertir metadata del agente a un DTO
    MetadataDTO thisMetadata = MetadataDTO.fromJsonNode(agent.getMetadata());

    // 3. Obtener las herramientas asociadas al agente
    List<McpTool> tools = thisMetadata.getTools();

    // Filtrar los MCP Servers y tools referenciados desde el agente
    List<UUID> mcpServerIds = tools.stream()
        .map(tool -> tool.getMcpServerId()) // Convertir el mcpServerId a UUID
        .distinct()
        .collect(Collectors.toList());

    // 4. Obtener los MCP Servers activos desde la base de datos que coincidan con
    // los MCP Server IDs
    List<McpServer> activeMcpServers = mcpServerRepository.findAll()
        .stream()
        .filter(mcpServer -> mcpServerIds.contains(mcpServer.getServerId()) // Comparar con UUID
            )
        .collect(Collectors.toList());

    AgentSnapshotDTO result = new AgentSnapshotDTO();

    result.setAgentId(agent.getAgentId());
    result.setDescription(agent.getDescription());
    result.setGithubRepoUrl(agent.getGithubRepoUrl());
    result.setName(agent.getName());
    result.setVersion(agent.getVersion());
    result.setConfig(new ConfigDTO());
    result.getConfig().setMcpServers(new ArrayList<McpServerDTO>());
    result.getConfig().setDiscovery(thisMetadata.getDiscovery());
    result.getConfig().setLlmInfo(thisMetadata.getLlms());
    result.setCreatedTs(agent.getCreatedTs().toString());
    result.setUpdatedTs(agent.getUpdatedTs().toString());

    // Procesar los MCP Servers activos
    for (McpServer mcpServer : activeMcpServers) {
      System.out.println("Procesando MCP Server:" + mcpServer.getName());
      McpServerDTO dto = new McpServerDTO();
      dto.setDescription(mcpServer.getDescription());
      dto.setDiscoveryUrl(mcpServer.getDiscoveryUrl());
      dto.setMcpServerId(mcpServer.getServerId());
      dto.setMcpServerName(mcpServer.getName());
      dto.setMcpServerVersion(mcpServer.getVersion());
      dto.setRepositoryUrl(mcpServer.getRepositoryUrl());
      dto.setVersion(mcpServer.getVersion());
      dto.setTools(new ArrayList<ToolDTO>());

      // Obtener las herramientas definidas en el MCP Server
      List<McpToolDefinition> toolDefinitions = McpToolNormalizer.normalize(mcpServer.getServerDoc(), om);
      // 1️⃣ Obtener tools del agente para ese server
      List<McpTool> agentTools = thisMetadata.getTools().stream()
          .filter(tool -> Objects.equals(tool.getMcpServerId(), mcpServer.getServerId()))
          .toList();

      // 2️⃣ Extraer nombres en un Set (lookup O(1))
      Set<String> agentToolNames = agentTools.stream()
          .map(McpTool::getToolName)
          .collect(Collectors.toSet());

      // 3️⃣ Procesar solo las toolDefinitions que estén en el agente
      for (McpToolDefinition tooldef : toolDefinitions) {

        if (!agentToolNames.contains(tooldef.getName())) {
          continue;
        }

        System.out.println("Procesando Tool: " + tooldef.getName());

        ToolDTO newToolDef = new ToolDTO();
        newToolDef.setArguments(tooldef.getArguments());
        newToolDef.setDescription(tooldef.getDescription());
        newToolDef.setVersion(tooldef.getVersion());
        newToolDef.setName(tooldef.getName());

        dto.getTools().add(newToolDef);
      }

      result.getConfig().getMcpServers().add(dto);
    }

    return result;
  }

  public AgentDTO updateAgent(UUID id, AgentDTO agentDTO)
      throws Exception {
    // Buscar el agente existente por ID
    Optional<Agent> existingAgent = agentRepository.findById(id);
    if (!existingAgent.isPresent()) {
      throw new RuntimeException("Agent not found");
    }

    // Validar que no exista otro agente con el mismo nombre y versión, excepto el
    // que estamos actualizando
    Optional<Agent> agentWithSameNameAndVersion = agentRepository.findByNameAndVersion(agentDTO.getName(),
        agentDTO.getVersion());
    if (agentWithSameNameAndVersion.isPresent() && !agentWithSameNameAndVersion.get().getAgentId().equals(id)) {
      throw new IllegalArgumentException("An agent with the same name and version already exists.");
    }

    // Deserializar la metadata JSON a MetadataDTO
    MetadataDTO metadata = agentDTO.getMetadata();

    // Validar que las tools declaradas existan en la registry
    for (MetadataDTO.McpTool tool : metadata.getTools()) {

      // Buscar el MCP Server correspondiente por ID
      Optional<McpServer> mcpServer = mcpServerRepository.findById(tool.getMcpServerId());

      if (!mcpServer.isPresent()) {
        throw new IllegalArgumentException("MCP Server with ID " + tool.getMcpServerId() + " does not exist.");
      }

      tool.setMcpServerName(mcpServer.get().getName());
      tool.setMcpServerVersion(mcpServer.get().getVersion());

      // Obtener el serverDoc que contiene las herramientas del MCP Server
      JsonNode serverDoc = mcpServer.get().getServerDoc();

      // Verificar que la herramienta esté presente en el serverDoc
      boolean toolFound = false;
      if (serverDoc != null && serverDoc.has("tools")) {
        JsonNode toolsNode = serverDoc.get("tools");

        // Iterar sobre las herramientas y verificar si la herramienta requerida está
        // presente
        for (JsonNode toolNode : toolsNode) {
          String toolName = toolNode.get("name").asText();
          if (toolName.equals(tool.getToolName())) {
            toolFound = true;
            break;
          }
        }
      }

      if (!toolFound) {
        throw new IllegalArgumentException(
            "Tool with name " + tool.getToolName() + " does not exist in MCP Server with ID " + tool.getMcpServerId());
      }
    }

    // testear el endpoint es alcanzable

    Agent thisAgent = existingAgent.get();
    Agent updatedAgent = agentDTO.toEntity();

    thisAgent.setName(updatedAgent.getName());
    thisAgent.setDescription(updatedAgent.getDescription());
    thisAgent.setGithubRepoUrl(updatedAgent.getGithubRepoUrl());
    thisAgent.setVersion(updatedAgent.getVersion());
    thisAgent.setMetadata(updatedAgent.getMetadata());
    thisAgent.setUpdatedTs(Instant.now());

    Agent savedAgent = agentRepository.save(thisAgent);

    // Convertir la entidad actualizada a DTO y devolver
    return AgentDTO.fromEntity(savedAgent);
  }

  /**
   * Lógica para importar un agente desde una definición completa
   * (AgentSnapshotDTO).
   * 
   * @param agentSnapshot La definición del agente a importar.
   */
  public void importAgent(AgentSnapshotDTO agentSnapshot) {
    if (agentSnapshot == null) {
      throw new IllegalArgumentException("Agent definition cannot be null.");
    }

    // Verificamos si el agente ya existe con el mismo ID
    Optional<Agent> existingAgent = agentRepository.findById(agentSnapshot.getAgentId());
    if (existingAgent.isPresent()) {
      // Si el agente ya existe, lanzamos una excepción o actualizamos según se desee
      throw new IllegalArgumentException("Agent with this ID already exists.");
    }

    // Crear el nuevo agente
    Agent agent = new Agent();
    agent.setAgentId(agentSnapshot.getAgentId());
    agent.setName(agentSnapshot.getName());
    agent.setDescription(agentSnapshot.getDescription());
    agent.setVersion(agentSnapshot.getVersion());
    agent.setGithubRepoUrl(agentSnapshot.getGithubRepoUrl());

    // Validar y crear los MCP Servers si no existen
    validateAndCreateMcpServers(agentSnapshot);

    // Validar el endpoint del agente
    validateAgentEndpoint(agentSnapshot);

    // Procesar la metadata, llms y discovery según el modelo
    processMetadata(agent, agentSnapshot);

    // Guardar el nuevo agente en la base de datos
    agentRepository.save(agent);
  }

  /**
   * Validar y crear los MCP Servers mencionados en la definición del agente.
   * Si no existen, los creamos y probamos su accesibilidad.
   * 
   * @param agentSnapshot La definición del agente con los MCP Servers.
   */
  private void validateAndCreateMcpServers(AgentSnapshotDTO agentSnapshot) {
    if (agentSnapshot.getConfig() != null && agentSnapshot.getConfig().getMcpServers() != null) {

      for (McpServerDTO mcpServerDTO : agentSnapshot.getConfig().getMcpServers()) {

        Optional<McpServer> existingMcpServer = mcpServerRepository.findByNameAndVersion(
            mcpServerDTO.getMcpServerName(), mcpServerDTO.getVersion());

        if (!existingMcpServer.isPresent()) {
          // Si el MCP Server no existe, lo creamos
          System.out.println("Server with Name and version=" +
              mcpServerDTO.getMcpServerName() + ":"
              + mcpServerDTO.getVersion() + " does not exists, creating,...");

          McpServer mcpServer = new McpServer();

          mcpServer.setName(mcpServerDTO.getMcpServerName());
          mcpServer.setVersion(mcpServerDTO.getVersion());
          mcpServer.setDescription(mcpServerDTO.getDescription());
          mcpServer.setRepositoryUrl(mcpServerDTO.getRepositoryUrl());
          mcpServer.setDiscoveryUrl(mcpServerDTO.getDiscoveryUrl());

          McpServerTestConnectionResponse testConnectionResult = mcpConnectionService
              .testConnection(mcpServer.getDiscoveryUrl());

          mcpServerRepository.save(mcpServer);
        }
      }
    }
  }

  /**
   * Validar que el agente sea alcanzable en el endpoint definido.
   * Si no es accesible, lanzamos un error.
   * 
   * @param agentSnapshot La definición del agente con el endpoint.
   */
  private void validateAgentEndpoint(AgentSnapshotDTO agentSnapshot) {
    if (agentSnapshot.getConfig() != null && agentSnapshot.getConfig().getDiscovery() != null) {
      String endpoint = agentSnapshot.getConfig().getDiscovery().getEndpoint();

      McpServerTestConnectionResponse testResponse = mcpConnectionService.testConnection(endpoint);

      if (!testResponse.isOk())
        throw new IllegalArgumentException(
            "Agent with ID " + agentSnapshot.getAgentId() + " is not reachable at the defined endpoint: " + endpoint);

    }
  }

  /**
   * Procesar la metadata del agente (llms y discovery).
   * 
   * @param agent         El agente que estamos importando.
   * @param agentSnapshot La definición completa del agente.
   */
  private void processMetadata(Agent agent, AgentSnapshotDTO agentSnapshot) {
    if (agentSnapshot.getConfig() != null) {
      MetadataDTO thisMetadata = MetadataDTO.fromJsonNode(agent.getMetadata());

      // Aquí procesamos llms y discovery
      if (agentSnapshot.getConfig().getDiscovery() != null) {

        thisMetadata.getDiscovery().setEndpoint(
            agentSnapshot.getConfig().getDiscovery().getEndpoint());
        thisMetadata.getDiscovery().setMethod(
            agentSnapshot.getConfig().getDiscovery().getMethod());
        thisMetadata.getDiscovery().setProtocol(
            agentSnapshot.getConfig().getDiscovery().getProtocol());
        thisMetadata.getDiscovery().setQueryParam(
            agentSnapshot.getConfig().getDiscovery().getQueryParam());

      }

      if (agentSnapshot.getConfig().getLlmInfo() != null)
        thisMetadata.setLlms(
            agentSnapshot.getConfig().getLlmInfo());

      agent.setMetadata(
          thisMetadata.convertToJsonNode());
    }

  }

}
