# Local Qdrant

This directory contains a local Qdrant instance for development and AI tooling around this repository.

Qdrant is not part of the SecureLanSuite runtime application. It is an infrastructure helper and should not be referenced from production Java modules unless a feature explicitly requires it later.

## Files

- [`docker-compose.yml`](docker-compose.yml) — local Qdrant service definition.
- [`qdrant_storage/`](qdrant_storage/) — persistent local Qdrant data, ignored by Git and AI indexing.
- [`snapshots/`](snapshots/) — optional local Qdrant snapshots, ignored by AI indexing and should stay out of Git.

## Ports

The compose file binds Qdrant to localhost only:

- REST API: `http://127.0.0.1:6333`
- gRPC API: `127.0.0.1:6334`

Binding to `127.0.0.1` avoids exposing the local vector database on the LAN.

## Start

From the repository root:

```powershell
docker compose -f .\infra\qdrant\docker-compose.yml up -d
```

Or from this directory:

```powershell
docker compose up -d
```

## Check status

```powershell
docker compose -f .\infra\qdrant\docker-compose.yml ps
```

REST health check:

```powershell
Invoke-RestMethod http://127.0.0.1:6333/healthz
```

Basic API check:

```powershell
Invoke-RestMethod http://127.0.0.1:6333/collections
```

## Stop

Stop the container without deleting local data:

```powershell
docker compose -f .\infra\qdrant\docker-compose.yml down
```

## Reset local Qdrant data

Stop Qdrant first, then delete the local storage directory:

```powershell
docker compose -f .\infra\qdrant\docker-compose.yml down
Remove-Item -Recurse -Force .\infra\qdrant\qdrant_storage
```

The next `up -d` command will create a fresh storage directory.

## Git and AI indexing

Local Qdrant data can be large and machine-specific. Keep these paths ignored:

```gitignore
/infra/qdrant/qdrant_storage/
/infra/qdrant/snapshots/
```

If Qdrant is used by an AI tool, store only local indexes here. Do not commit generated vector data, snapshots, API keys, or model caches.
