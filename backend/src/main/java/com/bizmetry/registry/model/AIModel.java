package com.bizmetry.registry.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID modelId; // Model ID (UUID)

    @NotBlank
    @Size(max = 256)
    @Column(nullable = false)
    private String modelName; // Model Name

    @Size(max = 4000)
    @Column
    private String modelDescription; // Model Description

    @Column
    private String provider; // provider

    // Getters and Setters
    public UUID getModelId() {
        return modelId;
    }

    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getProvider ()
    {
        return this.provider;
    }

    public void setProvider (String provider)
    {
        this.provider = provider;
    }


    // Constructor
    public AIModel() {}

    public AIModel(UUID modelId, String modelName, String modelDescription) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.modelDescription = modelDescription;
    }
}
