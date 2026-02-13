# Registry Backend (MVP)

Spring Boot + Postgres + MinIO (S3 compatible)

## Run (local)
- Ensure Postgres + MinIO running (via docker-compose from the root solution)
- Configure `application.yml` or env vars (docker profile)
- Build:
  mvn -q -DskipTests package
- Run:
  java -jar target/registry-backend-0.0.1-SNAPSHOT.jar

## API
- Tools: /v1/tools
- Agents: /v1/agents

## Upload agent image
POST /v1/agents/{agentKey}/versions/{version}/image (multipart form-data, field: file)

## Download agent image
GET /v1/agents/{agentKey}/versions/{version}/image
