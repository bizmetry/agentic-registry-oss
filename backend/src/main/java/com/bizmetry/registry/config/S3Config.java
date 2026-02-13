package com.bizmetry.registry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client(
      @Value("${registry.s3.endpoint}") String endpoint,
      @Value("${registry.s3.accessKey}") String accessKey,
      @Value("${registry.s3.secretKey}") String secretKey,
      @Value("${registry.s3.region}") String region
  ) {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .forcePathStyle(true) // important for MinIO
        .build();
  }
}
