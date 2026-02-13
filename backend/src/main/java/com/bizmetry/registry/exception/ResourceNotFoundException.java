package com.bizmetry.registry.exception;

// Excepción personalizada para cuando no se encuentra un recurso
public class ResourceNotFoundException extends RuntimeException {
    
    // Constructor con un mensaje de error
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
