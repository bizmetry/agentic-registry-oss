# Agentic Registry

**Agentic Registry** is a centralized solution designed for the management of both **MCP Servers** and **AI Agents**. It facilitates the registration, discovery, and management of agents and servers, providing a central hub for internal and external agents and tools to interact.

---

## 🚀 Features

### 🔹 MCP Server Module

**Agentic Integration**
- Register MCP Servers via REST API
- Pull MCP Server snapshot via REST API
- Health-check monitoring of MCP Servers

**UI-based Operations**
- Create and Edit MCP Servers through the UI
- Export an MCP Server definition in JSON format
- Import an existing MCP Server definition
- Delete an MCP Server definition and its associated tools
- Inspect and invoke MCP Server-associated tools

### 🔹 AI Agent Module

**AI Application Integration**
- Register AI Agents via REST API
- Discover Agents via advanced filtering/search/sorting
- Pull Agent snapshot via REST API
- Health-check monitoring of AI Agents

**UI-based Operations**
- Create and Edit AI Agent definitions
- Associate AI Models with AI Agents
- Associate endpoints with AI Agents
- Associate MCP Servers and tools with AI Agents
- Export / Import AI Agent definitions
- Delete AI Agent definitions

---

## 🎯 Purpose

Agentic Registry centralizes the **registration of MCP Servers** and **AI Agents**, enabling both **external and internal AI agents** to:

- Register themselves
- Discover other agents
- Pull their definition (snapshot) at startup
- Dynamically initialize LLMs and Tools

---

## 🌐 Application Access

### 🔹 Frontend UI

Accessible at:

- `http://localhost:3000`

Default credentials (when prompted):

- **Username:** `admin`
- **Password:** `admin`

### 🔹 Backend API

Accessible at:

- `http://localhost:8080`

The backend exposes the REST APIs for:
- Agent registration
- Agent discovery
- Agent snapshot retrieval
- MCP Server registration
- MCP Server snapshot retrieval
- Health checks

---

## 📡 REST API Endpoints

### 🔹 MCP Server API

**Register MCP Server**
- `POST /v1/api/registry/mcp-servers/register`

Example request body:

```json
{
  "name": "MCP Server 1",
  "version": "1.0",
  "description": "Main MCP Server",
  "discoveryUrl": "https://mcpserver1.example.com",
  "repositoryUrl": "https://github.com/org/mcp-server1"
}
```

### 🔹 AI Agent API

**Register Agent**
- `POST /v1/api/registry/agents/register`

**Discover Agents**
- `POST /v1/api/registry/agents/discover`

Supports:
- Sorting
- Searching (exact / partial match)
- Filtering (name, model, status)

**Get Agent Snapshot (Definition)**
- `GET /v1/api/registry/agents/{agentId}/definition`

---

## 📚 API Documentation (Swagger Files)

All Swagger/OpenAPI specifications are located under:

- `/swagger`

| File | Description |
|------|------------|
| `/swagger/agent-register.yaml` | Agent Registration API |
| `/swagger/agent-discover.yaml` | Agent Discovery API |
| `/swagger/agent-snapshot.yaml` | Agent Snapshot API |
| `/swagger/mcp-server-register.yaml` | MCP Server Registration API |

These YAML files can be imported into:
- Swagger UI
- Postman
- Insomnia
- Stoplight
- Redoc

---

## 🐳 Running with Docker

### 1️⃣ Clone the repository

```bash
git clone https://github.com/agentic-runtime/agentic-registry.git
cd agentic-registry
```

### 2️⃣ Start with Docker Compose

```bash
docker-compose up --build
```

After startup:
- Frontend UI → `http://localhost:3000`
- Backend API → `http://localhost:8080`

---

## 🤝 Contributing

1. Fork repository  
2. Create feature branch  
3. Commit changes  
4. Open Pull Request  

---

## 📄 License

MIT License — see LICENSE file for details.

---

## 👤 Maintainer

**Guillermo Wrba**  
Principal Solutions Architect  
GitHub: https://github.com/agentic-runtime  

---

Thank you for using Agentic Registry 🚀