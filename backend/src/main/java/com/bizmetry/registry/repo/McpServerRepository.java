package com.bizmetry.registry.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bizmetry.registry.model.McpServer;
import com.bizmetry.registry.model.McpServerStatus;

public interface McpServerRepository extends JpaRepository<McpServer, UUID> {

  // ✅ usado por healthcheck job
  List<McpServer> findByStatusNot(McpServerStatus status);

  // ✅ usado por import (upsert por unique constraint name+version)
  Optional<McpServer> findByNameAndVersion(String name, String version);

}
