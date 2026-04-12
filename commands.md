# Docker Commands

> Run all commands from the `bizcord-backtend/` directory (where `compose.yml` lives)
> unless the command relates to building an image (run from the relevant project folder).

---

## Build Images

Run each command from inside its project directory:

```bash
# Mediasoup SFU server
cd ../bizcord-mediasoup && docker build -t achrafyoussef/bizcord-mediasoup:latest .

# Spring Boot backend
docker build -t achrafyoussef/bizcord-backend:latest .

# Angular frontend
cd ../bizcord-frontend && docker build -t achrafyoussef/bizcord-frontend:latest .
```

### Tagged builds

```bash
docker build -t achrafyoussef/bizcord-backend:1.0.0 -t achrafyoussef/bizcord-backend:latest .
docker build -t achrafyoussef/bizcord-frontend:1.0.0 -t achrafyoussef/bizcord-frontend:latest .
docker build -t achrafyoussef/bizcord-mediasoup:1.0.0 -t achrafyoussef/bizcord-mediasoup:latest .
```

---

## Push Images

```bash
docker push achrafyoussef/bizcord-backend:latest
docker push achrafyoussef/bizcord-frontend:latest
docker push achrafyoussef/bizcord-mediasoup:latest

# Push a specific version tag
docker push achrafyoussef/bizcord-backend:1.0.0
docker push achrafyoussef/bizcord-frontend:1.0.0
docker push achrafyoussef/bizcord-mediasoup:1.0.0

# Push all tags at once (build with --all-tags shorthand not supported; use the two lines)
docker push achrafyoussef/bizcord-backend --all-tags
docker push achrafyoussef/bizcord-frontend --all-tags
docker push achrafyoussef/bizcord-mediasoup --all-tags
```

---

## Docker Compose

```bash
# Start all services (pull images, create containers)
docker compose up -d

# Start with a custom .env file
docker compose --env-file=.env up -d

# Stop all services
docker compose down

# Stop all services and remove volumes (wipe DB data)
docker compose down -v

# Rebuild specific service image and recreate container
docker compose up --build -d backend
docker compose up --build -d frontend
docker compose up --build -d mediasoup

# Restart a single running service (no rebuild)
docker compose restart backend
docker compose restart frontend
docker compose restart mediasoup
docker compose restart postgres

# Pull latest images and recreate all containers
docker compose pull && docker compose up -d --force-recreate

# Remove stopped containers, dangling images, unused networks
docker compose down --remove-orphans
docker image prune -f
```

---

## Logs

```bash
# All services, last 100 lines
docker compose logs --tail=100

# Follow logs for all services
docker compose logs -f

# Follow logs for a single service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mediasoup
docker compose logs -f postgres

# Last N lines for a service
docker compose logs --tail=50 backend

# Filter nginx logs for specific path
docker logs bizcord-frontend 2>&1 | grep -i "api\|ws\|error"
```

---

## Exec / Inspect

```bash
# Open a shell in a running container
docker exec -it bizcord-backend bash
docker exec -it bizcord-mediasoup sh
docker exec -it bizcord-frontend sh

# Connect to PostgreSQL inside the container
docker exec -it bizcord-postgres-db psql -U admin -d bizcord

# Inspect container networking
docker inspect bizcord-backend --format '{{json .NetworkSettings.Networks}}'

# Copy a file out of a container (e.g. logs, uploads)
docker cp bizcord-backend:/app/uploads ./uploads-backup
```

---

## Status & Monitoring

```bash
# Show running containers and health status
docker compose ps

# Live resource usage (CPU, memory, net I/O)
docker stats

# List all images
docker images | grep bizcord

# Check a container's environment variables
docker inspect bizcord-backend --format '{{range .Config.Env}}{{println .}}{{end}}'
```

---

## Database

```bash
# Dump the bizcord database
docker exec bizcord-postgres-db pg_dump -U admin bizcord > bizcord_backup.sql

# Restore from a dump
docker exec -i bizcord-postgres-db psql -U admin bizcord < bizcord_backup.sql

# Reset the database (wipe + recreate)
docker compose down -v && docker compose up -d
```

