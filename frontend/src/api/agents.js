import { api } from './client';

/**
 * Obtener todos los agentes con soporte para búsqueda y ordenación.
 * @param {Object} params Parámetros de búsqueda y ordenación.
 * @returns {Promise<Object>} La respuesta de la API.
 */
export const listAgents = async (params = {}) => {
  try {
    const response = await api.get('/v1/api/registry/agents', { params });
    return response.data; // Devuelve directamente los datos de los agentes
  } catch (error) {
    console.error("Error al obtener los agentes:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
};

/**
 * Crear un nuevo agente.
 * @param {Object} agentData Datos del agente a crear.
 * @returns {Promise<Object>} La respuesta de la API con el agente creado.
 */
export const createAgent = async (agentData) => {
  try {
    const response = await api.post('/v1/api/registry/agents', agentData);
    return response.data; // Devuelve el agente creado
  } catch (error) {
    console.error("Error al crear el agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
};

/**
 * Actualizar un agente existente.
 * @param {string} agentId ID del agente a actualizar.
 * @param {Object} agentData Datos del agente actualizados.
 * @returns {Promise<Object>} La respuesta de la API con el agente actualizado.
 */
export const updateAgent = async (agentId, agentData) => {
  try {
    const response = await api.put(`/v1/api/registry/agents/${agentId}`, agentData);
    return response.data; // Devuelve el agente actualizado
  } catch (error) {
    console.error("Error al actualizar el agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
};

/**
 * Eliminar un agente.
 * @param {string} agentId ID del agente a eliminar.
 * @returns {Promise<Object>} La respuesta de la API para la eliminación.
 */
export const deleteAgent = async (agentId) => {
  try {
    await api.delete(`/v1/api/registry/agents/${agentId}`);
    return true; // Indica que el agente fue eliminado correctamente
  } catch (error) {
    console.error("Error al eliminar el agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
};

/**
 * Obtener los detalles de un agente específico.
 * @param {string} agentId ID del agente a obtener.
 * @returns {Promise<Object>} La respuesta de la API con los detalles del agente.
 */
export const getAgentDetails = async (agentId) => {
  try {
    const response = await api.get(`/v1/api/registry/agents/${agentId}`);
    return response.data; // Devuelve los detalles del agente
  } catch (error) {
    console.error("Error al obtener los detalles del agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
};

/**
 * Obtener la definición completa de un agente (snapshot).
 * @param {string} agentId ID del agente.
 * @returns {Promise<Object>} La respuesta de la API con la definición completa del agente.
 */
export const getAgentDefinition = async (agentId) => {
  try {
    const response = await api.get(`/v1/api/registry/agents/${agentId}/definition`);
    return response.data; // Devuelve la definición completa del agente
  } catch (error) {
    console.error("Error al obtener la definición del agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }

};

/**
 * Importar un agente desde un archivo o definición externa.
 * @param {Object} agentSnapshot Datos completos del agente a importar.
 * @returns {Promise<Object>} La respuesta de la API con la confirmación de la importación.
 */
export const importAgents = async (agentSnapshot) => {
  try {
    // Realizamos una solicitud POST al endpoint de importación de agentes
    const response = await api.post('/v1/api/registry/agents/import', agentSnapshot);
    return response.data; // Devuelve los datos de la respuesta
  } catch (error) {
    console.error("Error al importar el agente:", error);
    throw error; // Lanzar el error para manejarlo en el componente
  }
}; 
