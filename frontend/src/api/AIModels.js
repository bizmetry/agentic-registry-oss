import { api } from "./client";

/**
 * GET /v1/api/registry/ai-models
 * params:
 * - search
 * - page (0-based)
 * - size
 * - sortBy (provider|modelName)
 * - sortDir (asc|desc)
 */
export const listAIModels = async (params = {}) => {
  const response = await api.get("/v1/api/registry/ai-models", { params });
  return response.data; // Page<AIModel>
};
