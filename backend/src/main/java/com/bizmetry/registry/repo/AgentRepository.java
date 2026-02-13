package com.bizmetry.registry.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bizmetry.registry.model.Agent;
import com.bizmetry.registry.model.AgentStatus;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    // Método para encontrar agentes por nombre y versión
    Optional<Agent> findByNameAndVersion(String name, String version);

    // Método para encontrar agentes cuyo estado no sea el especificado
    List<Agent> findByStatusNot(AgentStatus status);
}
