package com.bizmetry.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bizmetry.registry.mcp.HttpMcpClient;
import com.bizmetry.registry.mcp.McpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class McpClientConfig {

  @Bean
  public McpClient mcpClient(ObjectMapper mapper) {
    return new HttpMcpClient(mapper);
  }
}
