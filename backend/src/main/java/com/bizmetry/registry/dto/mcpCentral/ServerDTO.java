package com.bizmetry.registry.dto.mcpCentral;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos no reconocidos


public class ServerDTO {

    private String $schema;
    private String name;
    private String description;
    private String version;
    private RepositoryDTO repository; // Suponiendo que sea otro DTO para manejar el objeto 'repository'
    private String title;
    private List<RemoteDTO> remotes; // Suponiendo que remotes sea una lista de objetos de tipo RemoteDTO
    private List<PackageDTO> packages; // Suponiendo que packages sea una lista de objetos de tipo PackageDTO
    
    // Getters y Setters

    public String get$schema() {
        return $schema;
    }

    public void set$schema(String $schema) {
        this.$schema = $schema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public RepositoryDTO getRepository() {
        return repository;
    }

    public void setRepository(RepositoryDTO repository) {
        this.repository = repository;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<RemoteDTO> getRemotes() {
        return remotes;
    }

    public void setRemotes(List<RemoteDTO> remotes) {
        this.remotes = remotes;
    }

    public List<PackageDTO> getPackages() {
        return packages;
    }

    public void setPackages(List<PackageDTO> packages) {
        this.packages = packages;
    }
 
}
