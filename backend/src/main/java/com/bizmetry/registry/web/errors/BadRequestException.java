package com.bizmetry.registry.web.errors;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String msg) { super(msg); }
}
