package com.bizmetry.registry.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizmetry.registry.dto.agent.AgentDTO;
import com.bizmetry.registry.dto.agent.AgentDiscoverRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestResponse;
import com.bizmetry.registry.dto.agent.AgentSnapshotDTO;
import com.bizmetry.registry.dto.agent.register.AgentRegisterRequest;
import com.bizmetry.registry.exception.ErrorResponse;
import com.bizmetry.registry.exception.ResourceNotFoundException;
import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.repo.AgentRepository;
import com.bizmetry.registry.service.AgentDiscoveryService;
import com.bizmetry.registry.service.AgentService;

@RestController
@RequestMapping("/v1/api/registry/agents")
public class AgentController {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentService service;

    @Autowired
    private AgentDiscoveryService agentDiscoveryService;

    // 1. Consultar todos los agentes
    @GetMapping
    public ResponseEntity<?> getAllAgents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        try {
            List<Agent> agents;
            if (search != null && !search.isEmpty()) {
                agents = agentRepository.findAll()
                        .stream()
                        .filter(agent -> agent.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                agents = agentRepository.findAll();
            }

            if ("timestamp".equalsIgnoreCase(sortBy)) {
                agents.sort((a, b) -> "desc".equalsIgnoreCase(sortDir) ? b.getCreatedTs().compareTo(a.getCreatedTs())
                        : a.getCreatedTs().compareTo(b.getCreatedTs()));
            } else if ("name".equalsIgnoreCase(sortBy)) {
                agents.sort((a, b) -> "desc".equalsIgnoreCase(sortDir) ? b.getName().compareToIgnoreCase(a.getName())
                        : a.getName().compareToIgnoreCase(b.getName()));
            }

            List<AgentDTO> agentDTOs = agents.stream().map(AgentDTO::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(agentDTOs);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 2. Crear un agente
    @PostMapping
    public ResponseEntity<?> createAgent(@RequestBody AgentDTO agentDTO) {
        try {
            AgentDTO savedAgentDTO = service.createAgent(agentDTO);
            return ResponseEntity.status(201).body(savedAgentDTO);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 3. Eliminar un agente
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable UUID id) {
        try {
            Optional<Agent> agent = agentRepository.findById(id);
            if (agent.isPresent()) {
                agentRepository.delete(agent.get());
                return ResponseEntity.noContent().build();
            } else {
                throw new ResourceNotFoundException("Agent not found");
            }
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 4. Obtener detalles de un agente por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAgentById(@PathVariable UUID id) {
        try {
            Optional<Agent> agent = agentRepository.findById(id);
            return agent.map(a -> ResponseEntity.ok(AgentDTO.fromEntity(a)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 5. Probar el endpoint
    @PostMapping("/endpoint/test")
    public ResponseEntity<?> testEndpoint(@RequestBody AgentEndpointTestRequest req) {
        try {
            return ResponseEntity.ok(service.testEndpoint(req));
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 6. Actualizar un agente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable UUID id, @RequestBody AgentDTO agentDTO) {
        try {
            AgentDTO updatedAgentDTO = service.updateAgent(id, agentDTO);
            return ResponseEntity.ok(updatedAgentDTO);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 7. Obtener la definición (snapshot) de un agente
    @GetMapping("/{id}/definition")
    public ResponseEntity<?> getAgentDefinition(@PathVariable UUID id) {
        try {
            AgentSnapshotDTO agentSnapshot = service.getAgentSnapshot(id);
            return ResponseEntity.ok(agentSnapshot);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 8. Descubrir agentes
    @PostMapping("/discover")
    public ResponseEntity<?> discoverAgents(@RequestBody AgentDiscoverRequest request) {
        try {
            List<AgentDTO> agentDTOs = agentDiscoveryService.discoverAgents(request);
            return ResponseEntity.ok(agentDTOs);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 9. Importar un agente
    @PostMapping("/import")
    public ResponseEntity<?> importAgent(@RequestBody AgentSnapshotDTO agentSnapshot) {
        try {
            service.importAgent(agentSnapshot);
            return ResponseEntity.status(200).body("Agent imported successfully.");
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // 10. Registrar un agente
    @PostMapping("/register")
    public ResponseEntity<?> registerAgent(@RequestBody AgentRegisterRequest agentRegisterRequest) {
        try {
            AgentSnapshotDTO agentSnapshotDTO = agentDiscoveryService.registerAgent(agentRegisterRequest);
            return ResponseEntity.status(201).body(agentSnapshotDTO);
        } catch (Exception e) {
            return handleError(e);
        }
    }

    // Método para manejar errores y devolver un JSON de error
    private ResponseEntity<?> handleError(Exception e) {
        // Construimos la respuesta de error
        ErrorResponse errorResponse = new ErrorResponse("500", "Internal Server Error", e.getMessage());

        // Devolvemos un ResponseEntity con el error y el código adecuado
        if (e instanceof ResourceNotFoundException) {
            errorResponse = new ErrorResponse("404", "Not Found", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
