package com.bizmetry.registry.init;

import com.bizmetry.registry.model.AIModel;
import com.bizmetry.registry.repo.AIModelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Component
public class LiteLLMModelInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LiteLLMModelInitializer.class);

    private final AIModelRepository aiModelRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LiteLLMModelInitializer(AIModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            loadModelsFromLiteLLM();
        } catch (Exception e) {
            log.error("❌ Failed to initialize AI models from LiteLLM JSON", e);
        }
    }

    private void loadModelsFromLiteLLM() throws Exception {
        ClassPathResource resource = new ClassPathResource("litellm-model-info.json");

        if (!resource.exists()) {
            log.warn("⚠️ litellm_model_info.json not found, skipping AI model initialization");
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            int created = 0;
            int skipped = 0;

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();

                String modelName = entry.getKey();
                JsonNode modelNode = entry.getValue();

                String provider = modelNode.path("litellm_provider").asText(null);

                if (provider == null || provider.isBlank()) {
                    log.debug("Skipping model {} (no provider)", modelName);
                    continue;
                }

                // Idempotencia
                if (aiModelRepository.findByModelName(modelName).isPresent()) {
                    skipped++;
                    continue;
                }

                AIModel model = new AIModel();
                model.setModelId(UUID.randomUUID());
                model.setModelName(modelName);
                model.setProvider(provider);
                model.setModelDescription(
                    "Imported from LiteLLM (" + provider + ")"
                );

                aiModelRepository.save(model);
                created++;

                log.info("✅ AIModel registered: {} [{}]", modelName, provider);
            }

            log.info("🔹 LiteLLM AIModel init completed. created={}, skipped={}", created, skipped);
        }
    }
}
