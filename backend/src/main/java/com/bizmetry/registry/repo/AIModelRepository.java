package com.bizmetry.registry.repo;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bizmetry.registry.model.AIModel;

import java.util.Optional;
import java.util.UUID;

public interface AIModelRepository extends JpaRepository<AIModel, UUID> {
    // Aquí puedes definir métodos adicionales si es necesario
    // Por ejemplo, para buscar por nombre o descripción:
    Optional<AIModel> findByModelName(String modelName);

    Page<AIModel> findByModelNameContainingIgnoreCase(String modelName, Pageable pageable);
}
