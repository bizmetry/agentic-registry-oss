package com.bizmetry.registry.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bizmetry.registry.dto.agent.AgentEndpointTestRequest;
import com.bizmetry.registry.dto.agent.AgentEndpointTestResponse;
import com.bizmetry.registry.dto.agent.MetadataDTO;
import com.bizmetry.registry.dto.agent.MetadataDTO.McpTool;
import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.AgentStatus;
import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;
import com.bizmetry.registry.repo.AgentRepository;
import com.bizmetry.registry.repo.McpServerRepository;
import com.bizmetry.registry.service.AgentService;

@Component
public class AgentHealthcheckJob {

    private static final Logger log = LoggerFactory.getLogger(AgentHealthcheckJob.class);

    private final AgentRepository agentRepository;
    private final AgentService agentService;
    private final McpServerRepository mcpServerRepository;

    public AgentHealthcheckJob(AgentRepository agentRepository, AgentService agentService,
            McpServerRepository mcpServerRepository) {
        this.agentRepository = agentRepository;
        this.agentService = agentService;
        this.mcpServerRepository = mcpServerRepository;
    }

    /**
     * ✅ Corre:
     * - initialDelay: para que no dispare instantáneo mientras la app todavía
     * levanta
     * - fixedDelay: cada X ms después de terminar la corrida anterior
     */
    @Scheduled(initialDelayString = "${bizmetry.agent.healthcheck.initialDelayMs:2000}", fixedDelayString = "${bizmetry.agent.healthcheck.fixedDelayMs:60000}")
    @Transactional
    public void run() {
        long jobStart = System.currentTimeMillis();
        log.info("🔁 Agent healthcheck job started at {}", Instant.now());

        // Obtener los agentes activos
        List<Agent> agents = agentRepository.findAll();

        log.info("📡 Checking {} agents...", agents.size());

        int ok = 0;
        int failed = 0;

        for (Agent agent : agents) {
            long oneStart = System.currentTimeMillis();
            String agentId = String.valueOf(agent.getAgentId());

            log.info("➡️ Checking [{}] name='{}' status={}",
                    agentId, agent.getName(), agent.getStatus());

            try {
                refreshAgent(agent);

                ok++;
                log.info("✅ OK [{}] in {} ms (status now={})",
                        agentId,
                        (System.currentTimeMillis() - oneStart),
                        agent.getStatus());

            } catch (Exception e) {
                failed++;
                log.warn("❌ FAILED [{}] in {} ms -> {}",
                        agentId,
                        (System.currentTimeMillis() - oneStart),
                        safeMsg(e));
            }
        }

        log.info("🏁 Agent healthcheck job finished in {} ms | ok={} failed={}",
                (System.currentTimeMillis() - jobStart),
                ok,
                failed);
    }

    private void refreshAgent(Agent agent) {
        try {
            // 1) Crear un objeto de solicitud para el endpoint de prueba
            AgentEndpointTestRequest testRequest = new AgentEndpointTestRequest();
            MetadataDTO agentMetadata = MetadataDTO.fromJsonNode(agent.getMetadata());
            testRequest.setEndpoint(agentMetadata.getDiscovery().getEndpoint());
            testRequest.setProtocol(agentMetadata.getDiscovery().getProtocol());
            testRequest.setMethod(agentMetadata.getDiscovery().getMethod());
            testRequest.setQueryParam(agentMetadata.getDiscovery().getQueryParam());

            // validamos MCP tools. Si alguna MCP tool esta inactiva,
            // marcamos el agente como INACTIVO

            for (McpTool mcptool : agentMetadata.getTools()) {

                Optional<McpServer> thisMcpServer = mcpServerRepository.findById(mcptool.getMcpServerId());

                if (thisMcpServer.isPresent()) {
                    if (thisMcpServer.get().getStatus().equals(McpServerStatus.FAILED)) {
                        agent.setStatus(AgentStatus.INACTIVE);
                        agent.setUpdatedTs(Instant.now());
                        agentRepository.save(agent);
                        return;
                    }
                }
            }
            
            // 2) Usar el método testEndpoint de AgentService para verificar el endpoint
            AgentEndpointTestResponse testResponse = agentService.testEndpoint(testRequest);

            // 3) Verificar el resultado de la prueba
            if (testResponse.getStatus() == 200) {

                agent.setStatus(AgentStatus.ACTIVE);
                log.info("   🔄 Agent '{}' passed the healthcheck", agent.getName());

            } else {
                agent.setStatus(AgentStatus.INACTIVE);
                log.warn("   ❌ Agent '{}' failed the healthcheck with status: {}", agent.getName(),
                        testResponse.getStatus());
            }

            // 4) Actualizar los timestamps y guardar el agente
            agent.setUpdatedTs(Instant.now());
            agentRepository.save(agent);

        } catch (Exception e) {
            // Si ocurre un fallo, marcar el agente como FALLADO
            agent.setStatus(AgentStatus.INACTIVE);
            agent.setUpdatedTs(Instant.now());
            agentRepository.save(agent);

            // Re-lanzamos la excepción para que el trabajo lo registre como fallo
            throw e;
        }
    }

    /**
     * Convierte los mensajes de la excepción en un formato seguro para el log.
     * 
     * @param e La excepción a procesar.
     * @return Un mensaje de error seguro para el log.
     */
    private String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}
