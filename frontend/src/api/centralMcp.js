import { api } from "./client";

/**
 * Obtener los servidores MCP disponibles desde MCP Central.
 * @param {string|null} cursor Cursor de paginación (según tu backend; puede ser string).
 * @param {number} limit Cantidad de resultados por página.
 * @param {string} search Texto a buscar (opcional). Se envía como query param "search".
 * @returns {Promise<Object>} Lista/response con servidores MCP.
 */
export const getCentralMcpServers = async (cursor = null, limit = 10, search = "") => {
  try {
    const params = { limit };

    const c = (cursor ?? "").toString().trim();
    if (c.length > 0) params.cursor = c;

    const q = (search ?? "").trim();
    if (q.length > 0) params.search = q;

    const response = await api.get("/v1/api/registry/mcp-central/servers", { params });
    return response.data;
  } catch (error) {
    console.error("Error al obtener los servidores MCP:", error);
    throw error;
  }
};

/**
 * Importar un MCP Server desde MCP Central usando name + version
 * (acorde al controller: POST /v1/api/registry/mcp-central/servers/import)
 *
 * @param {string} serverName Nombre del server a importar
 * @param {string} serverVersion Versión del server a importar
 * @returns {Promise<Object>} Respuesta del backend (McpCentralServerImportResponse / McpServerResponse)
 */
export const importMcpServerFromMcpCentral = async (serverName, serverVersion) => {
  try {
    const payload = {
      serverName: (serverName ?? "").trim(),
      serverVersion: (serverVersion ?? "").trim(),
    };

    const response = await api.post("/v1/api/registry/mcp-central/servers/import", payload);
    return response.data;
  } catch (error) {
    console.error("Error al importar el MCP Server desde MCP Central:", error);
    throw error;
  }
};


