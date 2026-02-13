package com.bizmetry.registry.dto.agent;

public class AgentEndpointTestResponse {
  private boolean ok;
  private int status;
  private long latencyMs;
  private String message;
  private String responseSnippet;

  public boolean isOk() { return ok; }
  public void setOk(boolean ok) { this.ok = ok; }

  public int getStatus() { return status; }
  public void setStatus(int status) { this.status = status; }

  public long getLatencyMs() { return latencyMs; }
  public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public String getResponseSnippet() { return responseSnippet; }
  public void setResponseSnippet(String responseSnippet) { this.responseSnippet = responseSnippet; }
}
