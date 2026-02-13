package com.bizmetry.registry.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bizmetry.registry.dto.agent.AgentDTO; // Importamos el enum AgentStatus
import com.bizmetry.registry.dto.agent.AgentDiscoverRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestResponse;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO;
import com.bizmetry.registry.dto.agent.MetadataDTO;
import com.bizmetry.registry.dto.agent.MetadataDTO.Discovery;
import com.bizmetry.registry.dto.agent.MetadataDTO.LLMInfo;
import com.bizmetry.registry.dto.agent.MetadataDTO.McpTool;
import com.bizmetry.registry.dto.agent.register.AgentRegisterRequest;
import com.bizmetry.registry.model.AIModel;
import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.AgentStatus;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.repo.AIModelRepository;
import com.bizmetry.registry.repo.AgentRepository;
import com.bizmetry.registry.repo.McpServerRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AgentDiscoveryService {

    @Autowired
    private final AgentRepository agentRepository;

    @Autowired
    private final McpServerRepository mcpServerRepository;

    @Autowired
    private final AIModelRepository aiModelRepository;

    @Autowired
    private final AgentService agentService;

    /**
     * Método para descubrir agentes con sorting, searching, filtering y status.
     */

    public AgentDiscoveryService(
            McpServerRepository mcpServerRepository,
            AIModelRepository aiModelRepository,
            AgentRepository agentRepository,
            AgentService agentService

    ) {
        this.mcpServerRepository = mcpServerRepository;
        this.agentRepository = agentRepository;
        this.aiModelRepository = aiModelRepository;
        this.agentService = agentService;
    }

    public List<AgentDTO> discoverAgents(AgentDiscoverRequest request) {
        // Obtener todos los agentes de la base de datos y filtrar solo los activos
        List<Agent> agents = agentRepository.findAll()
                .stream()
                .filter(agent -> AgentStatus.ACTIVE.equals(agent.getStatus())) // Filtrar por estado "ACTIVE" por
                                                                               // defecto
                .collect(Collectors.toList());

        // 1. Aplicar sorting
        agents = applySorting(agents, request.getSorting());

        // 2. Aplicar searching
        agents = applySearching(agents, request.getSearching());

        // 3. Aplicar filtering
        agents = applyFiltering(agents, request.getFiltering());

        // Convertir las entidades Agent a DTOs
        return agents.stream()
                .map(AgentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 1. Método para aplicar sorting
    private List<Agent> applySorting(List<Agent> agents, AgentDiscoverRequest.Sorting sorting) {
        // Si no se especifica sorting, asumimos ascendente por nombre
        if (sorting == null) {
            sorting = new AgentDiscoverRequest.Sorting();
            sorting.setSortField("name");
            sorting.setSortDirection("ASC");
        }

        String sortField = sorting.getSortField();
        String sortDirection = sorting.getSortDirection();

        if ("timestamp".equalsIgnoreCase(sortField)) {
            agents.sort((a, b) -> "desc".equalsIgnoreCase(sortDirection) ? b.getCreatedTs().compareTo(a.getCreatedTs())
                    : a.getCreatedTs().compareTo(b.getCreatedTs()));
        } else if ("name".equalsIgnoreCase(sortField)) {
            agents.sort((a, b) -> "desc".equalsIgnoreCase(sortDirection) ? b.getName().compareToIgnoreCase(a.getName())
                    : a.getName().compareToIgnoreCase(b.getName()));
        }

        return agents;
    }

    // 2. Método para aplicar searching
    // 2. Método para aplicar searching
    private List<Agent> applySearching(List<Agent> agents, AgentDiscoverRequest.Searching searching) {
        // Si no se especifica búsqueda, no se aplica ningún filtro
        if (searching == null) {
            return agents;
        }

        String type = searching.getType();
        boolean caseSensitive = searching.isSensitive();
        List<String> terms = searching.getTerms();

        // Si no hay términos de búsqueda, retornamos la lista de agentes tal como está
        if (terms == null || terms.isEmpty()) {
            return agents;
        }

        // Inicializamos la lista de agentes filtrados
        List<Agent> filteredAgents = new ArrayList<>(agents);

        // Iteramos sobre los términos de búsqueda, y aplicamos lógica OR (cualquier
        // término que coincida)
        filteredAgents = filteredAgents.stream()
                .filter(agent -> matchesAnyTerm(agent, terms, type, caseSensitive))
                .collect(Collectors.toList());

        return filteredAgents;
    }

    // Método auxiliar para verificar si un agente coincide con cualquier término en
    // la lista
    private boolean matchesAnyTerm(Agent agent, List<String> terms, String type, boolean caseSensitive) {
        for (String term : terms) {
            final String searchTerm = caseSensitive ? term : term.toLowerCase(); // Aplica la sensibilidad a mayúsculas

            // Filtramos según el tipo de coincidencia (exacta o parcial)
            if ("exact_match".equalsIgnoreCase(type)) {
                if (matchesExact(agent, searchTerm, caseSensitive)) {
                    return true; // Si algún término coincide exactamente, el agente es válido
                }
            } else if ("partial_match".equalsIgnoreCase(type)) {
                if (matchesPartial(agent, searchTerm, caseSensitive)) {
                    return true; // Si algún término coincide parcialmente, el agente es válido
                }
            }
        }
        return false; // Si ningún término coincide, el agente no es válido
    }

    // Método auxiliar para la coincidencia exacta en los campos relevantes
    private boolean matchesExact(Agent agent, String term, boolean caseSensitive) {
        return (caseSensitive ? agent.getName().equals(term) : agent.getName().equalsIgnoreCase(term)) ||
                (caseSensitive ? agent.getDescription().equals(term) : agent.getDescription().equalsIgnoreCase(term)) ||
                (caseSensitive ? agent.getGithubRepoUrl().equals(term)
                        : agent.getGithubRepoUrl().equalsIgnoreCase(term))
                ||
                (caseSensitive ? (agent.getName() + " " + agent.getVersion()).equals(term)
                        : (agent.getName() + " " + agent.getVersion()).equalsIgnoreCase(term));
    }

    // Método auxiliar para la coincidencia parcial en los campos relevantes
    private boolean matchesPartial(Agent agent, String term, boolean caseSensitive) {
        return (caseSensitive ? agent.getName().contains(term) : agent.getName().toLowerCase().contains(term)) ||
                (caseSensitive ? agent.getDescription().contains(term)
                        : agent.getDescription().toLowerCase().contains(term))
                ||
                (caseSensitive ? agent.getGithubRepoUrl().contains(term)
                        : agent.getGithubRepoUrl().toLowerCase().contains(term))
                ||
                (caseSensitive ? (agent.getName() + " " + agent.getVersion()).contains(term)
                        : (agent.getName() + " " + agent.getVersion()).toLowerCase().contains(term));
    }

    // 3. Método para aplicar filtering
    private List<Agent> applyFiltering(List<Agent> agents, List<AgentDiscoverRequest.Filtering> filtering) {
        // Si no se especifica ningún filtro, no se aplica filtro
        if (filtering == null || filtering.isEmpty()) {
            return agents;
        }

        for (AgentDiscoverRequest.Filtering filter : filtering) {
            String field = filter.getField();
            List<String> values = filter.getValues();

            if ("name".equalsIgnoreCase(field)) {
                // Filtrado por nombre
                agents = agents.stream()
                        .filter(agent -> values.contains(agent.getName()))
                        .collect(Collectors.toList());
            } else if ("model".equalsIgnoreCase(field)) {
                // Filtrado por "model", buscando el valor en los LLMs de la metadata
                agents = agents.stream()
                        .filter(agent -> filterByModelInMetadata(

                                MetadataDTO.fromJsonNode(agent.getMetadata())

                                , values))
                        .collect(Collectors.toList());
            } else if ("status".equalsIgnoreCase(field)) {
                // Filtrado por "status"
                agents = agents.stream()
                        .filter(agent -> values.contains(agent.getStatus().toString())) // Filtramos por los valores de
                                                                                        // status
                        .collect(Collectors.toList());
            }
        }
        return agents;
    }

    // 3.1. Método para filtrar por "model" en metadata.llms (buscando
    // "family/model")
    private boolean filterByModelInMetadata(MetadataDTO metadata, List<String> filterValues) {
        if (metadata == null || metadata.getLlms() == null || filterValues == null || filterValues.isEmpty()) {
            return false;
        }

        // Recorremos los valores de filtro, buscando la coincidencia en los LLMs
        for (String filterValue : filterValues) {
            // Filtramos por la familia y el modelo dentro de los LLMs
            for (MetadataDTO.LLMInfo llm : metadata.getLlms()) {
                if (llm.getModelName().equalsIgnoreCase(filterValue)) {
                    return true; // Si coincide con el modelo, lo incluimos
                }
            }

        }

        return false; // Si no se encuentra ningún modelo que coincida
    }

    public AgentSnapshotDTO registerAgent(AgentRegisterRequest agentRegisterRequest) 
    throws Exception
    {
        // Verificar LLMs referenciados
        List<LLMInfo> modelList = new ArrayList<>();
        List<McpTool> tools = new ArrayList<>();

        Agent newAgent;
        Optional<Agent> thisAgent = agentRepository.findByNameAndVersion(agentRegisterRequest.getAgentName(),
                agentRegisterRequest.getVersion());

        if (!thisAgent.isPresent())
            newAgent = new Agent();
        else
            newAgent = thisAgent.get();

        // Validar la versión del agente
        if (!isValidVersion(agentRegisterRequest.getVersion())) {
            throw new IllegalArgumentException("Agent version must be in 'x.y' format (e.g., 1.0).");
        }

        // Validar los LLMs
        for (String llm : agentRegisterRequest.getLlms()) {
            
            Optional<AIModel> thisModel = aiModelRepository.findByModelName(llm);
            if (!thisModel.isPresent())
                throw new IllegalArgumentException("LLM with name " + llm + " does not exist.");

            AIModel thisLLM = thisModel.get();
            LLMInfo thisLLMInfo = new LLMInfo();
            thisLLMInfo.setId(thisLLM.getModelId());
            thisLLMInfo.setModelFamily(thisLLM.getProvider());
            thisLLMInfo.setModelName(thisLLM.getModelName());

            modelList.add(thisLLMInfo);
        }

        // Validar las herramientas y los MCP Servers asociados
        for (AgentRegisterRequest.Tool tool : agentRegisterRequest.getTools()) {
            // Validar la versión del MCP Server
            if (!isValidVersion(tool.getServerVersion())) {
                throw new IllegalArgumentException(
                        "Server version for tool " + tool.getToolName() + " must be in 'x.y' format (e.g., 1.0).");
            }

            Optional<McpServer> mcpServerOpt = mcpServerRepository.findByNameAndVersion(tool.getServerName(),
                    tool.getServerVersion());
            if (!mcpServerOpt.isPresent()) {
                throw new IllegalArgumentException("MCP Server with name " + tool.getServerName() + " and version "
                        + tool.getServerVersion() + " does not exist.");
            }

            // Verificar si la tool está presente en el MCP Server
            McpServer mcpServer = mcpServerOpt.get();
            if (tool.getToolName() != null) {
                if (!mcpServer.getServerDoc().has("tools") || !isToolInMcpServer(tool.getToolName(), mcpServer))
                    throw new IllegalArgumentException(
                            "Tool " + tool.getToolName() + " does not exist in MCP Server " + mcpServer.getName());

                McpTool thisNewTool = new McpTool();
                thisNewTool.setMcpServerId(mcpServer.getServerId());
                thisNewTool.setMcpServerName(mcpServer.getName());
                thisNewTool.setMcpServerVersion(mcpServer.getVersion());
                thisNewTool.setToolName(tool.getToolName());
                tools.add(thisNewTool);
            }
        }

        // Probar la URL de discovery
        AgentRegisterRequest.Discovery discovery = agentRegisterRequest.getDiscovery();

        // Verificar la URL de discovery
        Discovery agentDiscoveryInfo = testDiscoveryUrl(discovery);

        // Crear el agente
        newAgent.setName(agentRegisterRequest.getAgentName());
        newAgent.setDescription(agentRegisterRequest.getDescription());
        newAgent.setVersion(agentRegisterRequest.getVersion());
        newAgent.setGithubRepoUrl(agentRegisterRequest.getGithubRepoUrl());
        newAgent.setCreatedTs(Instant.now());
        newAgent.setStatus(AgentStatus.ACTIVE);
        newAgent.setUpdatedTs(Instant.now());

        MetadataDTO agentMetadata = new MetadataDTO();
        agentMetadata.setLlms(modelList);
        agentMetadata.setTools(tools);
        agentMetadata.setDiscovery(agentDiscoveryInfo);

        newAgent.setMetadata(agentMetadata.convertToJsonNode());

        // Guardar el agente en la base de datos
        Agent savedAgent = agentRepository.save(newAgent);

        AgentSnapshotDTO registerResponse = agentService.getAgentSnapshot(savedAgent.getAgentId());

        // Crear el DTO de snapshot para el agente registrado
        return registerResponse;
    }

  /**
 * Verifica si una versión está en el formato:
 * - 'x.y'
 * - 'x.y.z'
 * donde x, y, z son números enteros.
 *discov
 * @param version La versión a verificar.
 * @return true si el formato es válido, false si no lo es.
 */
private boolean isValidVersion(String version) {
    String versionPattern = "^\\d+\\.\\d+(\\.\\d+)?$";
    Pattern pattern = Pattern.compile(versionPattern);
    return pattern.matcher(version).matches();
}

    /**
     * Verifica si una tool existe dentro del MCP Server.
     *
     * @param toolName  El nombre de la herramienta.
     * @param mcpServer El MCP Server donde buscar la herramienta.
     * @return True si la tool está presente en el MCP Server, de lo contrario
     *         False.
     */
    private boolean isToolInMcpServer(String toolName, McpServer mcpServer) {
        Iterator<JsonNode> toolsIterator = mcpServer.getServerDoc().get("tools").elements();
        while (toolsIterator.hasNext()) {
            JsonNode toolNode = toolsIterator.next();
            if (toolNode.get("name").asText().equals(toolName)) {
                return true; // La tool fue encontrada
            }
        }
        return false; // La tool no fue encontrada
    }

    /**
     * Prueba la URL de discovery del agente.
     * 
     * @param discovery La configuración de discovery con la URL del agente.
     * @return True si la URL es accesible, de lo contrario False.
     */
    private Discovery testDiscoveryUrl(
            AgentRegisterRequest.Discovery discovery) 
            throws Exception {
        // Probar si la URL es accesible
        try {

            // Descomponer la URL en protocolo y endpoint
            String url = discovery.getUrl();
            String protocol = null;
            String endpoint = null;

            if (url != null && url.contains("://")) {
                String[] parts = url.split("://", 2);
                protocol = parts[0]; // El protocolo (http, https, etc.)
                endpoint = parts[1]; // El endpoint (example.com/api, etc.)
            } else
                throw new Exception("Bad formed URL:" + url);

            AgentEndpointTestRequest req = new AgentEndpointTestRequest();

            req.setEndpoint(endpoint);
            req.setProtocol(protocol);
            req.setMethod(discovery.getMethod());
            req.setQueryParam(discovery.getQueryParam());

            AgentEndpointTestResponse testResponse = agentService.testEndpoint(req);

            if (testResponse.getStatus() != 200)
                throw new Exception("Discovery failed for agent URL:"
                        + discovery.getUrl());

            Discovery discoveryInfo = new Discovery();
            discoveryInfo.setEndpoint(endpoint);
            discoveryInfo.setProtocol(protocol);
            discoveryInfo.setMethod(discovery.getMethod());
            discoveryInfo.setQueryParam(discovery.getQueryParam());

            return discoveryInfo;
        } catch (Exception e) {
             e.printStackTrace();;
             throw e;
                    }
    }
}
