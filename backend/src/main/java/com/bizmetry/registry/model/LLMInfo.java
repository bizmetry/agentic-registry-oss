package com.bizmetry.registry.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LLMInfo {

    @Column(nullable = false)
    private UUID id; // ID del modelo LLM

    @Column(nullable = false, length = 256)
    private String modelFamily; // Familia del modelo (por ejemplo, GPT-3, GPT-4, etc.)

    @Column(nullable = false, length = 256)
    private String modelName; // Nombre del modelo (por ejemplo, "GPT-3.5-turbo")

    // Constructor
    public LLMInfo(UUID id, String modelFamily, String modelName) {
        this.id = id;
        this.modelFamily = modelFamily;
        this.modelName = modelName;
    }

    // Getters & Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModelFamily() {
        return modelFamily;
    }

    public void setModelFamily(String modelFamily) {
        this.modelFamily = modelFamily;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
