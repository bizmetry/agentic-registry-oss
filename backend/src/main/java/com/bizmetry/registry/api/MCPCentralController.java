package com.bizmetry.registry.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizmetry.registry.dto.mcpCentral.McpCentralServerImportRequest;
import com.bizmetry.registry.dto.mcpCentral.McpCentralServerImportResponse;
import com.bizmetry.registry.dto.mcpCentral.McpServerSummaryDTO;
import com.bizmetry.registry.dto.mcpCentral.ServerDTO;
import com.bizmetry.registry.dto.mcpserver.McpServerResponse;
import com.bizmetry.registry.exception.ErrorResponse;
import com.bizmetry.registry.service.MCPCentralService;

@RestController
@RequestMapping("/v1/api/registry/mcp-central")
public class MCPCentralController {

    @Autowired
    private MCPCentralService mcpCentralService;

    /**
     * Obtener los servidores MCP disponibles desde el MCP Central Registry.
     * 
     * @param cursor Parámetro de paginación para obtener más servidores.
     * @param limit El límite de servidores a devolver.
     * @param search Texto a buscar (opcional). Se envía como query param "search".
     * @return Lista de servidores MCP con la información simplificada.
     */
    @GetMapping("/servers")
    public ResponseEntity<?> getMcpServers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        try {
            // Normalizar search
            String normalizedSearch = (search == null) ? null : search.trim();
            if (normalizedSearch != null && normalizedSearch.isEmpty()) {
                normalizedSearch = null;
            }

            // ✅ Ajuste: delegar a service con search
            // Necesitás agregar este overload en el service:
            // fetchMcpServers(String cursor, int limit, String search)
            List<McpServerSummaryDTO> mcpServers = mcpCentralService.fetchMcpServers(cursor, limit, normalizedSearch);

            return ResponseEntity.ok(mcpServers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("400", "Bad Request", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("500", "Internal Server Error", e.getMessage()));
        }
    }


    /**
     * Importa un MCP Server desde MCP Central usando name + version.
     */
    @PostMapping("/servers/import")
    public ResponseEntity<?> importMcpServer(@RequestBody McpCentralServerImportRequest request) {
        try {
         
            McpServerResponse imported =
                    mcpCentralService.importMcpServer(
                            request.getServerName(),
                            request.getServerVersion()
                    );

            return ResponseEntity.status(HttpStatus.CREATED).body(imported);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("400", "Bad Request", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("500", "Internal Server Error", e.getMessage()));
        }
    }

   
}
