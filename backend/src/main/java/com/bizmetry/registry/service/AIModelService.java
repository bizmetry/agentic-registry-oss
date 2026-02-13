package com.bizmetry.registry.service;

import com.bizmetry.registry.model.AIModel;
import com.bizmetry.registry.repo.AIModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AIModelService {

    private final AIModelRepository aiModelRepository;

    @Autowired
    public AIModelService(AIModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    /**
     * Listado paginado con search + sort.
     *
     * Default sort: provider ASC, modelName ASC
     * Sort override soporta: provider | modelName
     */
    public Page<AIModel> listAllAIModels(String search, Integer page, Integer size, String sortBy, String sortDir) {

        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 20 : Math.min(size, 200); // cap para evitar locuras

        // Default sort requerido
        Sort defaultSort = Sort.by(
                Sort.Order.asc("provider"),
                Sort.Order.asc("modelName")
        );

        Sort sort = defaultSort;

        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

            // allowlist fields
            if ("provider".equalsIgnoreCase(sortBy)) {
                sort = Sort.by(dir, "provider").and(Sort.by(Sort.Direction.ASC, "modelName"));
            } else if ("modelName".equalsIgnoreCase(sortBy) || "name".equalsIgnoreCase(sortBy)) {
                sort = Sort.by(dir, "modelName").and(Sort.by(Sort.Direction.ASC, "provider"));
            } else {
                sort = defaultSort; // fallback seguro
            }
        }

        Pageable pageable = PageRequest.of(p, s, sort);

        if (search != null && !search.trim().isBlank()) {
            return aiModelRepository.findByModelNameContainingIgnoreCase(search.trim(), pageable);
        }

        return aiModelRepository.findAll(pageable);
    }

    // Crear un nuevo modelo AI (si querés, luego agregamos provider acá)
    public AIModel createAIModel(String modelName, String modelDescription) {
        AIModel aiModel = new AIModel(UUID.randomUUID(), modelName, modelDescription);
        return aiModelRepository.save(aiModel);
    }

    public boolean deleteAIModel(UUID modelId) {
        Optional<AIModel> aiModel = aiModelRepository.findById(modelId);
        if (aiModel.isPresent()) {
            aiModelRepository.delete(aiModel.get());
            return true;
        }
        return false;
    }

    public Optional<AIModel> getAIModelById(UUID modelId) {
        return aiModelRepository.findById(modelId);
    }
}
