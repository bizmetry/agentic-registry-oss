# Agentic Registry — API Examples (Collection)

This document provides **ready-to-run** API examples for Agentic Registry (local dev).

## Base URLs

- **Backend API:** `http://localhost:8080`
- **Frontend UI:** `http://localhost:3000` (login: `admin` / `admin`)

---

## Conventions used in this collection

- Examples use `curl` (works on macOS/Linux).
- JSON bodies are formatted for readability.
- Some large responses (e.g., `tools`) are **truncated** with `...` to keep this doc usable.

---

## 1) Register an MCP Server

### Endpoint

`POST /v1/api/registry/mcp-servers/register`

Full URL:
`http://localhost:8080/v1/api/registry/mcp-servers/register`

### Request (JSON)

```json
{
  "name": "API Registered MCP Server",
  "version": "1.0",
  "description": "API Registered MCP Server",
  "discoveryUrl": "https://petstore.run.mcp.com.ai/mcp",
  "repositoryUrl": "https://github.com/example/repository"
}
```

### curl

```bash
curl -X POST "http://localhost:8080/v1/api/registry/mcp-servers/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "API Registered MCP Server",
    "version": "1.0",
    "description": "API Registered MCP Server",
    "discoveryUrl": "https://petstore.run.mcp.com.ai/mcp",
    "repositoryUrl": "https://github.com/example/repository"
  }'
```

### Response (example)

```json
{
  "serverId": "41c7b92f-9728-46f3-95a6-ce257d5a9273",
  "name": "API Registered MCP Server",
  "description": "API Registered MCP Server",
  "version": "1.0",
  "discoveryUrl": "https://petstore.run.mcp.com.ai/mcp",
  "repositoryUrl": "https://github.com/example/repository",
  "status": "ACTIVE",
  "createdTs": "2026-02-11T16:53:18.557676422Z",
  "updatedTs": "2026-02-11T16:53:18.557677505Z",
  "tools": [
    {
      "name": "uploadFile",
      "description": "uploads an image",
      "version": "1.0",
      "arguments": {
        "type": "object",
        "properties": {
          "petId": { "type": "string", "description": "ID of pet to update" },
          "additionalMetadata": { "type": "string", "description": "Additional data to pass to server" },
          "file": { "type": "string", "description": "file to upload" }
        }
      }
    },
    {
      "name": "addPet",
      "description": "Add a new pet to the store",
      "version": "1.0",
      "arguments": {
        "type": "object",
        "properties": {
          "body": { "type": "object" }
        },
        "required": ["body"]
      }
    }
    // ... many more tools omitted for brevity ...
  ]
}
```

### Notes

- The registry stores the MCP Server definition (including its tool catalog).
- The `tools` array can be large depending on the MCP server.

---

# Agentic Registry — Register AI Agent API

This document provides a complete example for registering an AI Agent in Agentic Registry.

## Endpoint

POST http://localhost:8080/v1/api/registry/agents/register

---

## Request Payload

```json
{
  "agentName": "PetStore Agent",
  "description": "An AI-powered assistant for PetStore clinical triage.",
  "version": "1.2",
  "githubRepoUrl": "https://github.com/org/ai-assistant",
  "discovery": {
    "url": "https://www.google.com",
    "method": "GET",
    "queryParam": "query"
  },
  "llms": [
    "azure/gpt-4o-mini"
  ],
  "tools": [
    {
      "serverName": "ai.exa/exa",
      "toolName": "web_search_exa",
      "serverVersion": "3.0.7"
    }
  ]
}
```

---

## curl Example

```bash
curl -X POST "http://localhost:8080/v1/api/registry/agents/register" \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "PetStore Agent",
    "description": "An AI-powered assistant for PetStore clinical triage.",
    "version": "1.2",
    "githubRepoUrl": "https://github.com/org/ai-assistant",
    "discovery": {
      "url": "https://www.google.com",
      "method": "GET",
      "queryParam": "query"
    },
    "llms": ["azure/gpt-4o-mini"],
    "tools": [
      {
        "serverName": "ai.exa/exa",
        "toolName": "web_search_exa",
        "serverVersion": "3.0.7"
      }
    ]
  }'
```

---

## Response Example

```json
{
  "agentId": "2d3982be-3bdd-41b2-964d-5bcc1a4870d8",
  "name": "PetStore Agent",
  "description": "An AI-powered assistant for PetStore clinical triage.",
  "version": "1.2",
  "githubRepoUrl": "https://github.com/org/ai-assistant",
  "createdTs": "2026-02-11T18:11:50.281337Z",
  "updatedTs": "2026-02-11T18:11:50.284035Z",
  "config": {
    "mcpServers": [
      {
        "mcpServerId": "7dac10e9-4a01-4f62-bc06-9b0d96e0560e",
        "mcpServerName": "ai.exa/exa",
        "mcpServerVersion": "3.0.7",
        "tools": [
          {
            "name": "web_search_exa",
            "version": null
          }
        ]
      }
    ],
    "llmInfo": [
      {
        "modelFamily": "azure",
        "modelName": "azure/gpt-4o-mini"
      }
    ],
    "discovery": {
      "method": "GET",
      "protocol": "https",
      "endpoint": "www.google.com",
      "queryParam": "query"
    }
  }
}
```

---

## Validation Rules

During registration the registry will:

- Validate agent version format (`x.y` or `x.y.z`)
- Validate referenced LLM existence
- Validate MCP Server and tool existence
- Validate discovery endpoint reachability
- Persist agent metadata as JSON
- Return a fully resolved snapshot

---

# Agentic Registry — Discover AI Agents API

This document provides a complete example for discovering AI Agents using filtering and sorting options.

---

## Endpoint

POST http://localhost:8080/v1/api/registry/agents/discover

---

## Request Payload

```json
{
  "sorting": {
    "sortDirection": "DESC",
    "sortField": "timestamp"
  },
  "searching": {
    "terms": [
      "Pet"
    ],
    "type": "partial_match",
    "sensitive": false
  }
}
```

---

## curl Example

```bash
curl -X POST "http://localhost:8080/v1/api/registry/agents/discover" \
  -H "Content-Type: application/json" \
  -d '{
    "sorting": {
      "sortDirection": "DESC",
      "sortField": "timestamp"
    },
    "searching": {
      "terms": ["Pet"],
      "type": "partial_match",
      "sensitive": false
    }
  }'
```

---

## Response Example

```json
[
  {
    "agentId": "aa80a671-4b27-445a-96cf-4874dbac4081",
    "name": "PetStore Agent",
    "description": "An AI-powered assistant for PetStore clinical triage.",
    "version": "1.2",
    "githubRepoUrl": "https://github.com/org/ai-assistant",
    "metadata": {
      "llms": [
        {
          "id": "2a2afb7b-41f4-4dac-8b0b-36624cbe8f86",
          "modelFamily": "azure",
          "modelName": "azure/gpt-4o-mini"
        }
      ],
      "tools": [
        {
          "mcpServerId": "41c7b92f-9728-46f3-95a6-ce257d5a9273",
          "toolName": "addPet",
          "mcpServerName": "API Registered MCP Server",
          "mcpServerVersion": "1.0"
        },
        {
          "mcpServerId": "41c7b92f-9728-46f3-95a6-ce257d5a9273",
          "toolName": "updatePet",
          "mcpServerName": "API Registered MCP Server",
          "mcpServerVersion": "1.0"
        },
        {
          "mcpServerId": "41c7b92f-9728-46f3-95a6-ce257d5a9273",
          "toolName": "findPetsByStatus",
          "mcpServerName": "API Registered MCP Server",
          "mcpServerVersion": "1.0"
        }
      ],
      "discovery": {
        "method": "GET",
        "protocol": "HTTPS",
        "endpoint": "www.google.com",
        "queryParam": "query"
      }
    },
    "createdTs": "2026-02-11T16:59:02.639925Z",
    "updatedTs": "2026-02-11T17:35:21.935144Z",
    "status": "ACTIVE"
  }
]
```

---

## Behavior

The discover endpoint supports:

- Sorting by timestamp or other supported fields
- Direction: ASC or DESC
- Searching by terms (partial or exact match)
- Case sensitivity toggle
- Returns only matching agents
- Returns full metadata including LLMs, tools, and discovery configuration

---


## Appendix: Full tool catalog in responses (optional)

In your real response, the `tools` array may include a large list such as:
- `uploadFile`
- `addPet`
- `updatePet`
- `findPetsByStatus`
- `getPetById`
- `placeOrder`
- `loginUser`
- etc.

For day-to-day usage, it’s recommended to:
- Validate that **server registration succeeded**
- Confirm **status** is `ACTIVE`
- Inspect/select tools via UI (`http://localhost:3000`) or via the corresponding snapshot endpoints (if exposed)

---

## Next examples

When you paste the real examples for:
- **Register Agent**
- **Discover Agents**
- **Get Agent Snapshot**
- **Invoke Tool** (if applicable)

…I can extend this collection to include those endpoints with the same style (request, curl, response, notes).
