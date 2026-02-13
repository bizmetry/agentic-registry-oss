package com.bizmetry.registry.exception;

public class ErrorResponse {
    private String errorCode;
    private String errorDescription;
    private String message;

    public ErrorResponse(String errorCode, String errorDescription, String message) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.message = message;
    }

    // Getters y Setters
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
