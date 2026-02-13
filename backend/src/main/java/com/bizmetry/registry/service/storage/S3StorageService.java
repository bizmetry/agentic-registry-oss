package com.bizmetry.registry.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
public class S3StorageService implements StorageService {

  private final S3Client s3;
  private final String bucket;

  public S3StorageService(S3Client s3, @Value("${registry.s3.bucket}") String bucket) {
    this.s3 = s3;
    this.bucket = bucket;
  }

  @Override
  public String put(String objectKey, String contentType, InputStream in, long contentLength) {
    PutObjectRequest req = PutObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .contentType(contentType)
        .build();
    s3.putObject(req, RequestBody.fromInputStream(in, contentLength));
    return objectKey;
  }

  @Override
  public InputStream get(String objectKey) {
    GetObjectRequest req = GetObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .build();
    return s3.getObject(req);
  }

  @Override
  public void delete(String objectKey) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
  }

  @Override
  public boolean exists(String objectKey) {
    try {
      s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) return false;
      throw e;
    }
  }
}
