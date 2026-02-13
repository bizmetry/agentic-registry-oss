import { api } from "./client";

// Listado de servidores
export const listServers = (params) =>
  api.get("/v1/api/registry/mcp-servers", { params }).then(r => r.data);

// Crear un servidor
export const createServer = (p) => api.post("/v1/api/registry/mcp-servers", p).then(r => r.data);

// Actualizar un servidor
export const updateServer = (id, p) => api.put(`/v1/api/registry/mcp-servers/${id}`, p).then(r => r.data);

// Eliminar un servidor
export const deleteServer = (id) => api.delete(`/v1/api/registry/mcp-servers/${id}`);

// Obtener definición de un servidor
export const getServerDefinition = (serverId) =>
  api.get(`/v1/api/registry/mcp-servers/${serverId}/definition`)
     .then(r => r.data);

// Testear la conexión con un servidor
export const testConnection = (url) =>
  api.post("/v1/api/registry/mcp-servers/test-connection", { discoveryUrl: url }).then(r => r.data);

// Invocar herramienta en un servidor
export const invokeTool = (serverId, toolName, args, opts = {}) => {
  let token = (opts.bearerToken || "").trim();
  if (token.toLowerCase().startsWith("bearer ")) {
    token = token.slice(7).trim();
  }

  const payload = {
    args: args || {},
    timeoutMs: opts.timeoutMs ?? 30000,
    dryRun: !!opts.dryRun,
    auth: token ? { bearerToken: token } : undefined
  };

  return api
    .post(`/v1/api/registry/mcp-servers/${serverId}/tools/${encodeURIComponent(toolName)}/invoke`, payload)
    .then(r => r.data);
};

// ✅ Agregar el método de importación de MCP Servers desde un archivo JSON
export const importMcpServers = (payload, { dryRun = false, upsert = true } = {}) => {
  const body = { payload, dryRun, upsert };
  return api
    .post("/v1/api/registry/mcp-servers/import", body)
    .then(r => r.data);
};
