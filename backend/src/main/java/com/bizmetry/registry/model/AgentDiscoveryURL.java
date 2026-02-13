package com.bizmetry.registry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AgentDiscoveryURL {

    @Column(nullable = false)
    private String protocol; // Protocolo HTTP o HTTPS

    @Column(nullable = false)
    private String endpoint; // El endpoint donde el agente está expuesto

    @Column(nullable = false)
    private String httpMethod; // Método HTTP (POST, GET, etc.)

    @Column(nullable = false)
    private String queryParameter; // El parámetro de consulta (query parameter) a enviar

    // Getters & Setters
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getQueryParameter() {
        return queryParameter;
    }

    public void setQueryParameter(String queryParameter) {
        this.queryParameter = queryParameter;
    }
}
