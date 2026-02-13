package com.bizmetry.registry.service.storage;

import java.io.InputStream;

public interface StorageService {
  String put(String objectKey, String contentType, InputStream in, long contentLength);
  InputStream get(String objectKey);
  void delete(String objectKey);
  boolean exists(String objectKey);
}
