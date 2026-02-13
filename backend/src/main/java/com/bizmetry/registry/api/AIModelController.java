package com.bizmetry.registry.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bizmetry.registry.model.AIModel;
import com.bizmetry.registry.service.AIModelService;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/registry/ai-models")
public class AIModelController {

    private final AIModelService aiModelService;

    @Autowired
    public AIModelController(AIModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    /**
     * Listar modelos AI (paginado + search + sort)
     *
     * Query params:
     * - search: filtra por modelName (contains, ignoreCase)
     * - page: page index (0-based). default 0
     * - size: page size. default 20
     * - sortBy: "provider" | "modelName" (default provider+modelName)
     * - sortDir: "asc" | "desc" (default asc)
     */
    @GetMapping
    public ResponseEntity<Page<AIModel>> listAIModels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        Page<AIModel> aiModels = aiModelService.listAllAIModels(search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(aiModels);
    }

    // Crear un nuevo modelo AI
    @PostMapping
    public ResponseEntity<AIModel> createAIModel(
            @RequestParam String modelName,
            @RequestParam String modelDescription
    ) {
        AIModel newModel = aiModelService.createAIModel(modelName, modelDescription);
        return ResponseEntity.status(201).body(newModel);
    }

    // Eliminar un modelo AI por ID
    @DeleteMapping("/{modelId}")
    public ResponseEntity<Void> deleteAIModel(@PathVariable UUID modelId) {
        boolean isDeleted = aiModelService.deleteAIModel(modelId);
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

    // Obtener detalles de un modelo AI por ID
    @GetMapping("/{modelId}")
    public ResponseEntity<AIModel> getAIModelDetails(@PathVariable UUID modelId) {
        Optional<AIModel> aiModel = aiModelService.getAIModelById(modelId);
        return aiModel.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).build());
    }
}
