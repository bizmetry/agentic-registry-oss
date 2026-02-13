package com.bizmetry.registry.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient() {
    // WebClient normal (valida TLS)
    return WebClient.builder().build();
  }

  @Bean(name = "insecureWebClient")
  public WebClient insecureWebClient() {
    try {
      SslContext sslContext = SslContextBuilder
          .forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE) // ✅ trust all certs
          .build();

      HttpClient httpClient = HttpClient.create()
          .secure(ssl -> ssl.sslContext(sslContext));

      return WebClient.builder()
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .build();

    } catch (Exception e) {
      throw new IllegalStateException("Failed to create insecureWebClient", e);
    }
  }
}
