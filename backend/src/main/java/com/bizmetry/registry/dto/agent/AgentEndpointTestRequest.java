package com.bizmetry.registry.dto.agent;

public class AgentEndpointTestRequest {
  private String method;        // GET/POST/PUT
  private String protocol;      // HTTP/HTTPS
  private String endpoint;      // host[:port]
  private String queryParam;    // required
  private String bearerToken;   // required (según tu UI)
  private String query;         // e.g. "test"

  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }

  public String getProtocol() { return protocol; }
  public void setProtocol(String protocol) { this.protocol = protocol; }

  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

  public String getQueryParam() { return queryParam; }
  public void setQueryParam(String queryParam) { this.queryParam = queryParam; }

  public String getBearerToken() { return bearerToken; }
  public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }

  public String getQuery() { return query; }
  public void setQuery(String query) { this.query = query; }
}
